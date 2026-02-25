package com.loopers.domain.order

interface OrderRepository {
    fun save(order: OrderModel): OrderModel
    fun findById(id: Long): OrderModel?
}
