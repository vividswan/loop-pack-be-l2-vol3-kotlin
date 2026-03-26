package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.StreamerIssuedCouponModel
import org.springframework.data.jpa.repository.JpaRepository

interface StreamerIssuedCouponJpaRepository : JpaRepository<StreamerIssuedCouponModel, Long> {
    fun countByCouponId(couponId: Long): Long
    fun existsByMemberIdAndCouponId(memberId: Long, couponId: Long): Boolean
}
