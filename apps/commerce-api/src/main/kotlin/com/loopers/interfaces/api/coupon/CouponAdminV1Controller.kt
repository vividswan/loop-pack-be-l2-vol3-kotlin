package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AdminInfo
import com.loopers.interfaces.api.auth.AuthenticatedAdmin
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime

@RestController
@RequestMapping("/api-admin/v1/coupons")
class CouponAdminV1Controller(
    private val couponFacade: CouponFacade,
) : CouponAdminV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createCoupon(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        @RequestBody request: CouponAdminV1Dto.CreateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponResponse> {
        return couponFacade.createCoupon(
            name = request.name,
            type = request.type,
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = ZonedDateTime.parse(request.expiredAt),
        )
            .let { CouponAdminV1Dto.CouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{id}")
    override fun getCoupon(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        @PathVariable id: Long,
    ): ApiResponse<CouponAdminV1Dto.CouponResponse> {
        return couponFacade.getCoupon(id)
            .let { CouponAdminV1Dto.CouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getCoupons(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.CouponResponse>> {
        return couponFacade.getCoupons(pageable)
            .map { CouponAdminV1Dto.CouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/{id}")
    override fun updateCoupon(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        @PathVariable id: Long,
        @RequestBody request: CouponAdminV1Dto.UpdateRequest,
    ): ApiResponse<CouponAdminV1Dto.CouponResponse> {
        return couponFacade.updateCoupon(
            id = id,
            name = request.name,
            type = request.type,
            value = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = ZonedDateTime.parse(request.expiredAt),
        )
            .let { CouponAdminV1Dto.CouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{id}")
    override fun deleteCoupon(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        @PathVariable id: Long,
    ): ApiResponse<Any> {
        couponFacade.deleteCoupon(id)
        return ApiResponse.success()
    }

    @GetMapping("/{couponId}/issues")
    override fun getIssuedCoupons(
        @AuthenticatedAdmin adminInfo: AdminInfo,
        @PathVariable couponId: Long,
        pageable: Pageable,
    ): ApiResponse<Page<CouponAdminV1Dto.IssuedCouponResponse>> {
        return couponFacade.getIssuedCouponsByCouponId(couponId, pageable)
            .map { CouponAdminV1Dto.IssuedCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
