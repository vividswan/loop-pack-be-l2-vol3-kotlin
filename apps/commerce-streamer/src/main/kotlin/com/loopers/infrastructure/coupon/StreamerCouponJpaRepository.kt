package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.StreamerCouponModel
import org.springframework.data.jpa.repository.JpaRepository

interface StreamerCouponJpaRepository : JpaRepository<StreamerCouponModel, Long>
