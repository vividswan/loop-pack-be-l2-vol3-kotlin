package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetricsModel
import org.springframework.data.jpa.repository.JpaRepository

interface ProductMetricsJpaRepository : JpaRepository<ProductMetricsModel, Long> {
    fun findByProductId(productId: Long): ProductMetricsModel?
}
