package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.domain.order.OrderCommand

class OrderV1Dto {
    data class CreateRequest(
        val items: List<OrderItemRequest>,
    ) {
        data class OrderItemRequest(
            val productId: Long,
            val quantity: Int,
        )

        fun toCommands(): List<OrderCommand.CreateOrderItem> {
            return items.map {
                OrderCommand.CreateOrderItem(
                    productId = it.productId,
                    quantity = it.quantity,
                )
            }
        }
    }

    data class OrderResponse(
        val id: Long,
        val memberId: Long,
        val status: String,
        val totalPrice: Long,
        val orderItems: List<OrderItemResponse>,
    ) {
        data class OrderItemResponse(
            val productId: Long,
            val productName: String,
            val quantity: Int,
            val price: Long,
        ) {
            companion object {
                fun from(info: OrderInfo.OrderItemInfo): OrderItemResponse {
                    return OrderItemResponse(
                        productId = info.productId,
                        productName = info.productName,
                        quantity = info.quantity,
                        price = info.price,
                    )
                }
            }
        }

        companion object {
            fun from(info: OrderInfo): OrderResponse {
                return OrderResponse(
                    id = info.id,
                    memberId = info.memberId,
                    status = info.status,
                    totalPrice = info.totalPrice,
                    orderItems = info.orderItems.map { OrderItemResponse.from(it) },
                )
            }
        }
    }
}
