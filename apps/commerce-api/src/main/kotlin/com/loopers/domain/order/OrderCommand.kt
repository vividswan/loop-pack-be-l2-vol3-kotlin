package com.loopers.domain.order

class OrderCommand {
    data class CreateOrderItem(
        val productId: Long,
        val quantity: Int,
    )
}
