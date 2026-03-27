package com.loopers.domain.metrics

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "product_metrics",
    indexes = [Index(name = "idx_product_metrics_product_id", columnList = "product_id", unique = true)],
)
class ProductMetricsModel internal constructor(
    productId: Long,
) : BaseEntity() {

    @Column(name = "product_id", nullable = false, unique = true)
    var productId: Long = productId
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0L
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0L
        protected set

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = 0L
        protected set

    @Column(name = "sales_amount", nullable = false)
    var salesAmount: Long = 0L
        protected set

    fun incrementLikeCount() {
        this.likeCount++
    }

    fun decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--
    }

    fun incrementViewCount() {
        this.viewCount++
    }

    fun addOrderMetrics(count: Long, amount: Long) {
        this.orderCount += count
        this.salesAmount += amount
    }

    companion object {
        fun create(productId: Long): ProductMetricsModel {
            return ProductMetricsModel(productId = productId)
        }
    }
}
