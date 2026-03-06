package com.loopers.application.coupon

import com.loopers.domain.coupon.IssuedCouponModel

data class IssuedCouponInfo(
    val id: Long,
    val memberId: Long,
    val couponId: Long,
    val status: String,
    val usedAt: String?,
) {
    companion object {
        fun from(model: IssuedCouponModel): IssuedCouponInfo {
            return IssuedCouponInfo(
                id = model.id,
                memberId = model.memberId,
                couponId = model.couponId,
                status = model.status.name,
                usedAt = model.usedAt?.toString(),
            )
        }
    }
}
