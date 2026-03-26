package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.StreamerCouponIssueRequestModel
import org.springframework.data.jpa.repository.JpaRepository

interface StreamerCouponIssueRequestJpaRepository : JpaRepository<StreamerCouponIssueRequestModel, Long>
