package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(
    name = "orders",
    indexes = [Index(name = "idx_order_member_id", columnList = "member_id")],
)
class OrderModel internal constructor(
    memberId: Long,
) : BaseEntity() {

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = OrderStatus.CREATED
        protected set

    @Column(name = "total_price", nullable = false)
    var totalPrice: Long = 0L
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val orderItems: MutableList<OrderItemModel> = mutableListOf()

    fun addItem(item: OrderItemModel) {
        item.order = this
        orderItems.add(item)
        totalPrice += item.getTotalPrice()
    }

    fun cancel() {
        this.status = OrderStatus.CANCELLED
    }

    companion object {
        fun create(
            memberId: Long,
            items: List<OrderItemModel>,
        ): OrderModel {
            if (items.isEmpty()) {
                throw CoreException(ErrorType.BAD_REQUEST, OrderErrorCode.ITEMS_EMPTY)
            }
            val order = OrderModel(memberId = memberId)
            items.forEach { order.addItem(it) }
            return order
        }
    }
}
