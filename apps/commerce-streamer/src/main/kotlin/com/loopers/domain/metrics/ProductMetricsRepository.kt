package com.loopers.domain.metrics

interface ProductMetricsRepository {
    fun findByProductId(productId: Long): ProductMetricsModel?
    fun save(metrics: ProductMetricsModel): ProductMetricsModel
}
