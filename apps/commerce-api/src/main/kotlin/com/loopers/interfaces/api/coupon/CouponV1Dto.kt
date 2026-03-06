package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.IssuedCouponInfo

class CouponV1Dto {

    data class IssuedCouponResponse(
        val id: Long,
        val couponId: Long,
        val status: String,
        val usedAt: String?,
    ) {
        companion object {
            fun from(info: IssuedCouponInfo): IssuedCouponResponse {
                return IssuedCouponResponse(
                    id = info.id,
                    couponId = info.couponId,
                    status = info.status,
                    usedAt = info.usedAt,
                )
            }
        }
    }
}
