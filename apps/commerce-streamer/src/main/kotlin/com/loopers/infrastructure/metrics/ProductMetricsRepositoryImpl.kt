package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.domain.metrics.ProductMetricsRepository
import org.springframework.stereotype.Component

@Component
class ProductMetricsRepositoryImpl(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) : ProductMetricsRepository {

    override fun findByProductId(productId: Long): ProductMetricsModel? {
        return productMetricsJpaRepository.findByProductId(productId)
    }

    override fun save(metrics: ProductMetricsModel): ProductMetricsModel {
        return productMetricsJpaRepository.save(metrics)
    }
}
