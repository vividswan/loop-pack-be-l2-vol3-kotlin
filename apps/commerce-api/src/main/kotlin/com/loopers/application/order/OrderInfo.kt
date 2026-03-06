package com.loopers.application.order

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel

data class OrderInfo(
    val id: Long,
    val memberId: Long,
    val status: String,
    val totalPrice: Long,
    val orderItems: List<OrderItemInfo>,
) {
    data class OrderItemInfo(
        val productId: Long,
        val productName: String,
        val quantity: Int,
        val price: Long,
    ) {
        companion object {
            fun from(model: OrderItemModel): OrderItemInfo {
                return OrderItemInfo(
                    productId = model.productId,
                    productName = model.productName,
                    quantity = model.quantity,
                    price = model.price,
                )
            }
        }
    }

    companion object {
        fun from(model: OrderModel): OrderInfo {
            return OrderInfo(
                id = model.id,
                memberId = model.memberId,
                status = model.status.name,
                totalPrice = model.totalPrice,
                orderItems = model.orderItems.map { OrderItemInfo.from(it) },
            )
        }
    }
}
