package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponIssueRequestInfo

class CouponIssueV1Dto {

    data class IssueRequestResponse(
        val requestId: Long,
        val couponId: Long,
        val status: String,
        val failureReason: String?,
    ) {
        companion object {
            fun from(info: CouponIssueRequestInfo): IssueRequestResponse {
                return IssueRequestResponse(
                    requestId = info.id,
                    couponId = info.couponId,
                    status = info.status,
                    failureReason = info.failureReason,
                )
            }
        }
    }
}
