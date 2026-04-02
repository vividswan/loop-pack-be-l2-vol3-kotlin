package com.loopers.application.order

data class OrderCreatedEvent(
    val orderId: Long,
    val memberId: Long,
    val totalPrice: Long,
    val productIds: List<Long>,
)
