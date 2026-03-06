package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.domain.order.OrderCommand

class OrderV1Dto {
    data class CreateRequest(
        val items: List<OrderItemRequest>,
        val couponId: Long? = null,
    ) {
        data class OrderItemRequest(
            val productId: Long,
            val quantity: Int,
        )

        fun toCommand(): OrderCommand.CreateOrder {
            return OrderCommand.CreateOrder(
                items = items.map {
                    OrderCommand.CreateOrderItem(
                        productId = it.productId,
                        quantity = it.quantity,
                    )
                },
                couponId = couponId,
            )
        }
    }

    data class OrderResponse(
        val id: Long,
        val memberId: Long,
        val status: String,
        val totalPrice: Long,
        val originalPrice: Long,
        val discountAmount: Long,
        val couponId: Long?,
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
                    originalPrice = info.originalPrice,
                    discountAmount = info.discountAmount,
                    couponId = info.couponId,
                    orderItems = info.orderItems.map { OrderItemResponse.from(it) },
                )
            }
        }
    }
}
