package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface IssuedCouponRepository {
    fun save(issuedCoupon: IssuedCouponModel): IssuedCouponModel
    fun findById(id: Long): IssuedCouponModel?
    fun findByMemberIdAndCouponId(memberId: Long, couponId: Long): IssuedCouponModel?
    fun existsByMemberIdAndCouponId(memberId: Long, couponId: Long): Boolean
    fun findAllByMemberId(memberId: Long): List<IssuedCouponModel>
    fun findAllByMemberIdAndStatus(memberId: Long, status: CouponStatus): List<IssuedCouponModel>
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCouponModel>
}
