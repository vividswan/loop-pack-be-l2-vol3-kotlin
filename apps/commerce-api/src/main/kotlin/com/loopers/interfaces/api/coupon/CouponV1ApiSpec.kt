package com.loopers.interfaces.api.coupon

import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon", description = "쿠폰 API")
interface CouponV1ApiSpec {

    @Operation(summary = "쿠폰 발급 요청", description = "사용자가 쿠폰을 발급받습니다.")
    fun issueCoupon(
        @AuthenticatedMember memberInfo: MemberInfo,
        couponId: Long,
    ): ApiResponse<CouponV1Dto.IssuedCouponResponse>

    @Operation(summary = "내 쿠폰 목록 조회", description = "내가 보유한 쿠폰 목록을 조회합니다.")
    fun getMyCoupons(
        @AuthenticatedMember memberInfo: MemberInfo,
        status: String?,
    ): ApiResponse<List<CouponV1Dto.IssuedCouponResponse>>
}
