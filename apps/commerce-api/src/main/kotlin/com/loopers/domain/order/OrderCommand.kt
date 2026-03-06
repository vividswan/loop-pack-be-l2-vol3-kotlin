package com.loopers.domain.order

class OrderCommand {
    data class CreateOrderItem(
        val productId: Long,
        val quantity: Int,
    )

    data class CreateOrder(
        val items: List<CreateOrderItem>,
        val couponId: Long?,
    )
}
