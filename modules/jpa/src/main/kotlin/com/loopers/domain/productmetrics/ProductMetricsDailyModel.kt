package com.loopers.domain.productmetrics

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "product_metrics_daily",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_metrics_product_date", columnNames = ["product_id", "metric_date"]),
    ],
    indexes = [
        Index(name = "idx_metrics_daily_date", columnList = "metric_date"),
    ],
)
class ProductMetricsDailyModel internal constructor(
    productId: Long,
    metricDate: LocalDate,
    likeDelta: Long,
    viewDelta: Long,
    orderDelta: Long,
    salesDelta: Long,
) : BaseEntity() {

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "metric_date", nullable = false)
    var metricDate: LocalDate = metricDate
        protected set

    @Column(name = "like_delta", nullable = false)
    var likeDelta: Long = likeDelta
        protected set

    @Column(name = "view_delta", nullable = false)
    var viewDelta: Long = viewDelta
        protected set

    @Column(name = "order_delta", nullable = false)
    var orderDelta: Long = orderDelta
        protected set

    @Column(name = "sales_delta", nullable = false)
    var salesDelta: Long = salesDelta
        protected set

    companion object {
        fun create(
            productId: Long,
            metricDate: LocalDate,
            likeDelta: Long,
            viewDelta: Long,
            orderDelta: Long,
            salesDelta: Long,
        ): ProductMetricsDailyModel {
            return ProductMetricsDailyModel(
                productId = productId,
                metricDate = metricDate,
                likeDelta = likeDelta,
                viewDelta = viewDelta,
                orderDelta = orderDelta,
                salesDelta = salesDelta,
            )
        }
    }
}
