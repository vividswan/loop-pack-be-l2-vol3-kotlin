package com.loopers.job.ranking

import com.loopers.batch.job.ranking.MonthlyRankingJobConfig
import com.loopers.domain.productrank.MvProductRankMonthlyModel
import com.loopers.infrastructure.productrank.MvProductRankMonthlyJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

@SpringBootTest
@SpringBatchTest
@TestPropertySource(
    properties = [
        "spring.batch.job.name=${MonthlyRankingJobConfig.JOB_NAME}",
        "spring.batch.job.enabled=false",
    ],
)
class MonthlyRankingJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(MonthlyRankingJobConfig.JOB_NAME) private val job: Job,
    private val mvMonthlyJpaRepository: MvProductRankMonthlyJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    dataSource: DataSource,
) {
    private val jdbcTemplate = JdbcTemplate(dataSource)

    @BeforeEach
    fun setUp() {
        jobLauncherTestUtils.job = job
        createProductMetricsDailyTableIfNotExists()
        jdbcTemplate.execute("TRUNCATE TABLE product_metrics_daily")
        databaseCleanUp.truncateAllTables()
    }

    private fun createProductMetricsDailyTableIfNotExists() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS product_metrics_daily (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                metric_date DATE NOT NULL,
                like_delta BIGINT NOT NULL DEFAULT 0,
                view_delta BIGINT NOT NULL DEFAULT 0,
                order_delta BIGINT NOT NULL DEFAULT 0,
                sales_delta BIGINT NOT NULL DEFAULT 0,
                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                deleted_at DATETIME(6) NULL,
                UNIQUE KEY uk_product_date (product_id, metric_date)
            )
            """.trimIndent(),
        )
    }

    private fun insertDailyMetrics(
        productId: Long,
        metricDate: LocalDate,
        likeDelta: Long,
        viewDelta: Long,
        orderDelta: Long,
        salesDelta: Long,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO product_metrics_daily
                (product_id, metric_date, like_delta, view_delta, order_delta, sales_delta, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
            """.trimIndent(),
            productId,
            Date.valueOf(metricDate),
            likeDelta,
            viewDelta,
            orderDelta,
            salesDelta,
        )
    }

    private fun buildJobParameters(requestDate: LocalDate = LocalDate.now()): JobParameters {
        return JobParametersBuilder()
            .addLocalDate("requestDate", requestDate)
            .addLong("run.id", System.nanoTime())
            .toJobParameters()
    }

    @DisplayName("product_metrics_daily 일별 변화량을 가중치 기반으로 집계하여 월간 랭킹 MV에 적재한다.")
    @Test
    fun aggregatesDailyMetricsIntoMonthlyRanking() {
        // arrange
        val today = LocalDate.now()
        insertDailyMetrics(1L, today, 100, 500, 50, 1_000_000)
        insertDailyMetrics(2L, today, 200, 300, 30, 500_000)
        insertDailyMetrics(3L, today, 50, 1000, 80, 2_000_000)

        // act
        val jobExecution = jobLauncherTestUtils.launchJob(buildJobParameters())

        // assert
        val periodKey = MvProductRankMonthlyModel.periodKeyFrom(LocalDate.now())
        val rankings = mvMonthlyJpaRepository.findAll().sortedBy { it.rankPosition }
        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rankings).hasSize(3) },
            { assertThat(rankings[0].productId).isEqualTo(3L) },
            { assertThat(rankings[0].rankPosition).isEqualTo(1) },
            { assertThat(rankings[1].productId).isEqualTo(1L) },
            { assertThat(rankings[1].rankPosition).isEqualTo(2) },
            { assertThat(rankings[2].productId).isEqualTo(2L) },
            { assertThat(rankings[2].rankPosition).isEqualTo(3) },
            { assertThat(rankings.all { it.periodKey == periodKey }).isTrue() },
        )
    }

    @DisplayName("여러 날에 걸친 일별 변화량을 합산하여 점수를 계산한다.")
    @Test
    fun aggregatesMultipleDaysOfDeltasIntoScore() {
        // arrange
        val today = LocalDate.now()
        insertDailyMetrics(1L, today, 60, 300, 30, 600_000)
        insertDailyMetrics(1L, today.minusDays(10), 40, 200, 20, 400_000)

        // act
        val jobExecution = jobLauncherTestUtils.launchJob(buildJobParameters())

        // assert
        val rankings = mvMonthlyJpaRepository.findAll()
        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rankings).hasSize(1) },
            { assertThat(rankings[0].score).isEqualTo(105.0) },
            { assertThat(rankings[0].likeCount).isEqualTo(100L) },
            { assertThat(rankings[0].viewCount).isEqualTo(500L) },
            { assertThat(rankings[0].orderCount).isEqualTo(50L) },
        )
    }

    @DisplayName("30일 범위 밖의 데이터는 집계에서 제외된다.")
    @Test
    fun excludesDataOutsideMonthlyRange() {
        // arrange
        val today = LocalDate.now()
        insertDailyMetrics(1L, today, 100, 500, 50, 1_000_000)
        insertDailyMetrics(2L, today.minusDays(30), 200, 300, 30, 500_000) // 범위 밖

        // act
        val jobExecution = jobLauncherTestUtils.launchJob(buildJobParameters())

        // assert
        val rankings = mvMonthlyJpaRepository.findAll()
        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rankings).hasSize(1) },
            { assertThat(rankings[0].productId).isEqualTo(1L) },
        )
    }

    @DisplayName("동일 기간에 배치를 재실행하면, 기존 데이터를 삭제하고 새로 적재한다 (멱등성).")
    @Test
    fun rerunningJobForSamePeriodIsIdempotent() {
        // arrange
        val today = LocalDate.now()
        insertDailyMetrics(1L, today, 100, 500, 50, 1_000_000)

        val requestDate = LocalDate.now()
        jobLauncherTestUtils.launchJob(buildJobParameters(requestDate))

        // act
        val jobExecution = jobLauncherTestUtils.launchJob(buildJobParameters(requestDate))

        // assert
        val rankings = mvMonthlyJpaRepository.findAll()
        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(rankings).hasSize(1) },
        )
    }
}
