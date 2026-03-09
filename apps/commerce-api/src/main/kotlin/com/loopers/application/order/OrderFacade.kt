package com.loopers.application.order

import com.loopers.domain.coupon.CouponErrorCode
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.OrderCommand
import com.loopers.domain.order.OrderService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val couponService: CouponService,
) {
    @Transactional
    fun createOrder(memberId: Long, command: OrderCommand.CreateOrder): OrderInfo {
        val order = orderService.createOrder(memberId, command.items)

        if (command.couponId != null) {
            val issuedCoupon = couponService.getIssuedCoupon(command.couponId)

            if (issuedCoupon.memberId != memberId) {
                throw CoreException(ErrorType.FORBIDDEN, CouponErrorCode.OWNER_MISMATCH)
            }

            val coupon = couponService.getCoupon(issuedCoupon.couponId)
            val discountAmount = coupon.calculateDiscount(order.originalPrice)
            order.applyDiscount(discountAmount, coupon.id)
            couponService.useIssuedCoupon(command.couponId)
        }

        return OrderInfo.from(order)
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderInfo {
        val order = orderService.getOrder(orderId)
        return OrderInfo.from(order)
    }
}
