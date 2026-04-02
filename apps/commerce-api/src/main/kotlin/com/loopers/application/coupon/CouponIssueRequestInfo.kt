package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponIssueRequestModel

data class CouponIssueRequestInfo(
    val id: Long,
    val memberId: Long,
    val couponId: Long,
    val status: String,
    val failureReason: String?,
) {
    companion object {
        fun from(model: CouponIssueRequestModel): CouponIssueRequestInfo {
            return CouponIssueRequestInfo(
                id = model.id,
                memberId = model.memberId,
                couponId = model.couponId,
                status = model.status.name,
                failureReason = model.failureReason,
            )
        }
    }
}
