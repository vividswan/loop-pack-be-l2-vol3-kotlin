package com.loopers.domain.coupon

interface CouponIssueRequestRepository {
    fun save(request: CouponIssueRequestModel): CouponIssueRequestModel
    fun findById(id: Long): CouponIssueRequestModel?
}
