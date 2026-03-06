package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponInfo
import com.loopers.application.coupon.IssuedCouponInfo
import com.loopers.domain.coupon.CouponType

class CouponAdminV1Dto {
    data class CreateRequest(
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long,
        val expiredAt: String,
    )

    data class UpdateRequest(
        val name: String,
        val type: CouponType,
        val value: Long,
        val minOrderAmount: Long,
        val expiredAt: String,
    )

    data class CouponResponse(
        val id: Long,
        val name: String,
        val type: String,
        val value: Long,
        val minOrderAmount: Long,
        val expiredAt: String,
    ) {
        companion object {
            fun from(info: CouponInfo): CouponResponse {
                return CouponResponse(
                    id = info.id,
                    name = info.name,
                    type = info.type,
                    value = info.value,
                    minOrderAmount = info.minOrderAmount,
                    expiredAt = info.expiredAt,
                )
            }
        }
    }

    data class IssuedCouponResponse(
        val id: Long,
        val memberId: Long,
        val couponId: Long,
        val status: String,
        val usedAt: String?,
    ) {
        companion object {
            fun from(info: IssuedCouponInfo): IssuedCouponResponse {
                return IssuedCouponResponse(
                    id = info.id,
                    memberId = info.memberId,
                    couponId = info.couponId,
                    status = info.status,
                    usedAt = info.usedAt,
                )
            }
        }
    }
}
