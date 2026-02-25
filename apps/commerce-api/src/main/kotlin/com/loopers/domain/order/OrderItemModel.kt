package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "order_item",
    indexes = [Index(name = "idx_order_item_order_id", columnList = "order_id")],
)
class OrderItemModel internal constructor(
    productId: Long,
    productName: String,
    quantity: Int,
    price: Long,
) : BaseEntity() {

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "product_name", nullable = false)
    var productName: String = productName
        protected set

    @Column(name = "quantity", nullable = false)
    var quantity: Int = quantity
        protected set

    @Column(name = "price", nullable = false)
    var price: Long = price
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: OrderModel? = null
        internal set

    fun getTotalPrice(): Long = price * quantity

    init {
        validateQuantity(quantity)
    }

    companion object {
        fun create(
            productId: Long,
            productName: String,
            quantity: Int,
            price: Long,
        ): OrderItemModel {
            return OrderItemModel(
                productId = productId,
                productName = productName,
                quantity = quantity,
                price = price,
            )
        }

        private fun validateQuantity(quantity: Int) {
            if (quantity < 1) {
                throw CoreException(ErrorType.BAD_REQUEST, OrderErrorCode.QUANTITY_NOT_POSITIVE)
            }
        }
    }
}
