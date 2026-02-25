package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import org.springframework.stereotype.Component

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: OrderModel): OrderModel {
        return orderJpaRepository.save(order)
    }

    override fun findById(id: Long): OrderModel? {
        return orderJpaRepository.findById(id).orElse(null)
    }
}
