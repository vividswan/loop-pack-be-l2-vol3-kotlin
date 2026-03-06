package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AdminInfo
import com.loopers.interfaces.api.auth.AuthenticatedAdmin
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Tag(name = "Coupon Admin", description = "쿠폰 관리자 API")
interface CouponAdminV1ApiSpec {

    @Operation(summary = "쿠폰 생성", description = "새로운 쿠폰 템플릿을 생성합니다.")
    fun createCoupon(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        request: CouponAdminV1Dto.CreateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponResponse>

    @Operation(summary = "쿠폰 상세 조회", description = "쿠폰 템플릿 상세 정보를 조회합니다.")
    fun getCoupon(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        id: Long,
    ): ApiResponse<CouponAdminV1Dto.CouponResponse>

    @Operation(summary = "쿠폰 목록 조회", description = "쿠폰 템플릿 목록을 페이지네이션으로 조회합니다.")
    fun getCoupons(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.CouponResponse>>

    @Operation(summary = "쿠폰 수정", description = "쿠폰 템플릿을 수정합니다.")
    fun updateCoupon(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        id: Long,
        request: CouponAdminV1Dto.UpdateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponResponse>

    @Operation(summary = "쿠폰 삭제", description = "쿠폰 템플릿을 삭제합니다.")
    fun deleteCoupon(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        id: Long,
    ): ApiResponse<Any>

    @Operation(summary = "쿠폰 발급 내역 조회", description = "특정 쿠폰의 발급 내역을 조회합니다.")
    fun getIssuedCoupons(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        couponId: Long,
        pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.IssuedCouponResponse>>
}
