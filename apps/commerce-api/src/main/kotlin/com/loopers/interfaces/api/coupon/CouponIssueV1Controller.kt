package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponIssueFacade
import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/coupons/first-come")
class CouponIssueV1Controller(
    private val couponIssueFacade: CouponIssueFacade,
) : CouponIssueV1ApiSpec {

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    override fun requestIssue(
        @AuthenticatedMember memberInfo: MemberInfo,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponIssueV1Dto.IssueRequestResponse> {
        return couponIssueFacade.requestFirstComeCouponIssue(memberInfo.id, couponId)
            .let { CouponIssueV1Dto.IssueRequestResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/requests/{requestId}")
    override fun getIssueRequestStatus(
        @AuthenticatedMember memberInfo: MemberInfo,
        @PathVariable requestId: Long,
    ): ApiResponse<CouponIssueV1Dto.IssueRequestResponse> {
        return couponIssueFacade.getIssueRequestStatus(requestId)
            .let { CouponIssueV1Dto.IssueRequestResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
