package com.loopers.application.order

import com.loopers.domain.order.OrderService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
) {
    @Transactional
    fun createOrder(memberId: Long, items: List<OrderService.OrderItemCommand>): OrderInfo {
        val order = orderService.createOrder(memberId, items)
        return OrderInfo.from(order)
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderInfo {
        val order = orderService.getOrder(orderId)
        return OrderInfo.from(order)
    }
}
