package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponFacade(
    private val couponService: CouponService,
) {
    @Transactional
    fun createCoupon(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long,
        expiredAt: ZonedDateTime,
    ): CouponInfo {
        val coupon = CouponModel.create(
            name = name,
            type = type,
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        )
        val savedCoupon = couponService.createCoupon(coupon)
        return CouponInfo.from(savedCoupon)
    }

    @Transactional(readOnly = true)
    fun getCoupon(id: Long): CouponInfo {
        val coupon = couponService.getCoupon(id)
        return CouponInfo.from(coupon)
    }

    @Transactional(readOnly = true)
    fun getCoupons(pageable: Pageable): Page<CouponInfo> {
        return couponService.getCoupons(pageable).map { CouponInfo.from(it) }
    }

    @Transactional
    fun updateCoupon(
        id: Long,
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long,
        expiredAt: ZonedDateTime,
    ): CouponInfo {
        val coupon = couponService.updateCoupon(id, name, type, value, minOrderAmount, expiredAt)
        return CouponInfo.from(coupon)
    }

    @Transactional
    fun deleteCoupon(id: Long) {
        couponService.deleteCoupon(id)
    }

    @Transactional
    fun issueCoupon(memberId: Long, couponId: Long): IssuedCouponInfo {
        val issuedCoupon = couponService.issueCoupon(memberId, couponId)
        return IssuedCouponInfo.from(issuedCoupon)
    }

    @Transactional(readOnly = true)
    fun getMyIssuedCoupons(memberId: Long): List<IssuedCouponInfo> {
        return couponService.getMyIssuedCoupons(memberId).map { IssuedCouponInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMyAvailableCoupons(memberId: Long): List<IssuedCouponInfo> {
        return couponService.getMyAvailableCoupons(memberId).map { IssuedCouponInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getIssuedCouponsByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCouponInfo> {
        return couponService.getIssuedCouponsByCouponId(couponId, pageable).map { IssuedCouponInfo.from(it) }
    }
}
