package com.loopers.interfaces.api.coupon

import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Coupon Issue", description = "선착순 쿠폰 발급 API")
interface CouponIssueV1ApiSpec {

    @Operation(summary = "선착순 쿠폰 발급 요청", description = "Kafka를 통한 비동기 쿠폰 발급을 요청합니다.")
    fun requestIssue(
        @AuthenticatedMember memberInfo: MemberInfo,
        couponId: Long,
    ): ApiResponse<CouponIssueV1Dto.IssueRequestResponse>

    @Operation(summary = "발급 요청 상태 조회", description = "선착순 쿠폰 발급 요청의 처리 상태를 조회합니다.")
    fun getIssueRequestStatus(
        @AuthenticatedMember memberInfo: MemberInfo,
        requestId: Long,
    ): ApiResponse<CouponIssueV1Dto.IssueRequestResponse>
}
