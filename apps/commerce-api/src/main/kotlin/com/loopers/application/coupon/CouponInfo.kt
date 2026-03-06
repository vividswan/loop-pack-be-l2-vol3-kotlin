package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponModel

data class CouponInfo(
    val id: Long,
    val name: String,
    val type: String,
    val value: Long,
    val minOrderAmount: Long,
    val expiredAt: String,
) {
    companion object {
        fun from(model: CouponModel): CouponInfo {
            return CouponInfo(
                id = model.id,
                name = model.name,
                type = model.type.name,
                value = model.value,
                minOrderAmount = model.minOrderAmount,
                expiredAt = model.expiredAt.toString(),
            )
        }
    }
}
