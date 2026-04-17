package com.loopers.batch.job.ranking.step

import com.loopers.batch.job.ranking.RankingScoreWeights
import com.loopers.batch.job.ranking.WeeklyRankingJobConfig
import com.loopers.batch.job.ranking.dto.ProductMetricsRow
import com.loopers.domain.productrank.MvProductRankWeeklyModel
import com.loopers.infrastructure.productrank.MvProductRankWeeklyJpaRepository
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

@StepScope
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJobConfig.JOB_NAME)
@Component
class WeeklyRankingTasklet(
    @param:Value("#{jobParameters['requestDate']}") private val requestDate: LocalDate,
    dataSource: DataSource,
    private val mvWeeklyJpaRepository: MvProductRankWeeklyJpaRepository,
) : Tasklet {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val periodKey = MvProductRankWeeklyModel.periodKeyFrom(requestDate)
        val startDate = requestDate.minusDays(WEEKLY_RANGE_DAYS)

        mvWeeklyJpaRepository.deleteAllByPeriodKey(periodKey)

        val rows = jdbcTemplate.query(
            PRODUCT_METRICS_DAILY_SQL,
            ROW_MAPPER,
            RankingScoreWeights.LIKE,
            RankingScoreWeights.VIEW,
            RankingScoreWeights.ORDER,
            Date.valueOf(startDate),
            Date.valueOf(requestDate),
        )

        val entities = rows.mapIndexed { index, row ->
            MvProductRankWeeklyModel.create(
                productId = row.productId,
                score = row.score,
                rankPosition = index + 1,
                likeCount = row.likeCount,
                viewCount = row.viewCount,
                orderCount = row.orderCount,
                salesAmount = row.salesAmount,
                periodKey = periodKey,
            )
        }
        mvWeeklyJpaRepository.saveAll(entities)

        return RepeatStatus.FINISHED
    }

    companion object {
        private const val WEEKLY_RANGE_DAYS = 6L

        private val PRODUCT_METRICS_DAILY_SQL = """
            SELECT product_id,
                   SUM(like_delta) as like_count,
                   SUM(view_delta) as view_count,
                   SUM(order_delta) as order_count,
                   SUM(sales_delta) as sales_amount,
                   (SUM(like_delta) * ? + SUM(view_delta) * ? + SUM(order_delta) * ?) as score
            FROM product_metrics_daily
            WHERE deleted_at IS NULL
              AND metric_date >= ?
              AND metric_date <= ?
            GROUP BY product_id
            ORDER BY score DESC
            LIMIT 100
        """.trimIndent()

        private val ROW_MAPPER = RowMapper { rs, _ ->
            ProductMetricsRow(
                productId = rs.getLong("product_id"),
                likeCount = rs.getLong("like_count"),
                viewCount = rs.getLong("view_count"),
                orderCount = rs.getLong("order_count"),
                salesAmount = rs.getLong("sales_amount"),
                score = rs.getDouble("score"),
            )
        }
    }
}
