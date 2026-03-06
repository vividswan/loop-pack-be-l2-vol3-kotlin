package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.IssuedCouponModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponModel, Long> {
    fun findByMemberIdAndCouponId(memberId: Long, couponId: Long): IssuedCouponModel?
    fun existsByMemberIdAndCouponId(memberId: Long, couponId: Long): Boolean
    fun findAllByMemberId(memberId: Long): List<IssuedCouponModel>
    fun findAllByMemberIdAndStatus(memberId: Long, status: CouponStatus): List<IssuedCouponModel>
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCouponModel>
}
