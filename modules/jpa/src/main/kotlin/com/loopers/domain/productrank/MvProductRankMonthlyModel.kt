package com.loopers.domain.productrank

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity
@Table(
    name = "mv_product_rank_monthly",
    indexes = [
        Index(name = "idx_mv_monthly_period_rank", columnList = "period_key, rank_position"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_mv_monthly_product_period", columnNames = ["product_id", "period_key"]),
    ],
)
class MvProductRankMonthlyModel internal constructor(
    productId: Long,
    score: Double,
    rankPosition: Int,
    likeCount: Long,
    viewCount: Long,
    orderCount: Long,
    salesAmount: Long,
    periodKey: String,
) : BaseEntity() {

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "score", nullable = false)
    var score: Double = score
        protected set

    @Column(name = "rank_position", nullable = false)
    var rankPosition: Int = rankPosition
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = orderCount
        protected set

    @Column(name = "sales_amount", nullable = false)
    var salesAmount: Long = salesAmount
        protected set

    @Column(name = "period_key", nullable = false, length = 10)
    var periodKey: String = periodKey
        protected set

    companion object {
        private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM")

        fun periodKeyFrom(date: LocalDate): String {
            return date.format(MONTH_FORMAT)
        }

        fun create(
            productId: Long,
            score: Double,
            rankPosition: Int,
            likeCount: Long,
            viewCount: Long,
            orderCount: Long,
            salesAmount: Long,
            periodKey: String,
        ): MvProductRankMonthlyModel {
            return MvProductRankMonthlyModel(
                productId = productId,
                score = score,
                rankPosition = rankPosition,
                likeCount = likeCount,
                viewCount = viewCount,
                orderCount = orderCount,
                salesAmount = salesAmount,
                periodKey = periodKey,
            )
        }
    }
}
