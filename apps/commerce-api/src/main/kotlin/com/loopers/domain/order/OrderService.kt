package com.loopers.domain.order

import com.loopers.domain.product.ProductErrorCode
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
) {
    fun createOrder(memberId: Long, orderItemCommands: List<OrderItemCommand>): OrderModel {
        val orderItems = orderItemCommands.map { command ->
            val product = productRepository.findById(command.productId)
                ?: throw CoreException(ErrorType.NOT_FOUND, ProductErrorCode.NOT_FOUND)

            product.decreaseStock(command.quantity)

            OrderItemModel.create(
                productId = product.id,
                productName = product.name,
                quantity = command.quantity,
                price = product.price,
            )
        }

        val order = OrderModel.create(memberId = memberId, items = orderItems)
        return orderRepository.save(order)
    }

    fun getOrder(id: Long): OrderModel {
        return orderRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, OrderErrorCode.NOT_FOUND)
    }

    data class OrderItemCommand(
        val productId: Long,
        val quantity: Int,
    )
}
