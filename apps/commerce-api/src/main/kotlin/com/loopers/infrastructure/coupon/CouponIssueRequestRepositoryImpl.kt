package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequestModel
import com.loopers.domain.coupon.CouponIssueRequestRepository
import org.springframework.stereotype.Component

@Component
class CouponIssueRequestRepositoryImpl(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {

    override fun save(request: CouponIssueRequestModel): CouponIssueRequestModel {
        return couponIssueRequestJpaRepository.save(request)
    }

    override fun findById(id: Long): CouponIssueRequestModel? {
        return couponIssueRequestJpaRepository.findById(id).orElse(null)
    }
}
