package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import org.springframework.data.jpa.repository.JpaRepository

interface CouponJpaRepository : JpaRepository<CouponModel, Long>
