package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) : CouponV1ApiSpec {

    @PostMapping("/api/v1/coupons/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    override fun issueCoupon(
        @AuthenticatedMember memberInfo: MemberInfo,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.IssuedCouponResponse> {
        return couponFacade.issueCoupon(memberInfo.id, couponId)
            .let { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/api/v1/users/me/coupons")
    override fun getMyCoupons(
        @AuthenticatedMember memberInfo: MemberInfo,
        @RequestParam(required = false) status: String?,
    ): ApiResponse<List<CouponV1Dto.IssuedCouponResponse>> {
        val coupons = when (status?.uppercase()) {
            "AVAILABLE" -> couponFacade.getMyAvailableCoupons(memberInfo.id)
            else -> couponFacade.getMyIssuedCoupons(memberInfo.id)
        }
        return coupons
            .map { CouponV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
