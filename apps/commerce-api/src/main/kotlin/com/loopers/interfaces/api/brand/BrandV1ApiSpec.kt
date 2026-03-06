package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Brand", description = "브랜드 API")
interface BrandV1ApiSpec {

    @Operation(summary = "브랜드 등록", description = "새로운 브랜드를 등록합니다.")
    fun register(request: BrandV1Dto.RegisterRequest): ApiResponse<BrandV1Dto.RegisterResponse>

    @Operation(summary = "브랜드 목록 조회", description = "전체 브랜드 목록을 조회합니다.")
    fun getBrands(): ApiResponse<List<BrandV1Dto.BrandResponse>>
}
