package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Product", description = "상품 API")
interface ProductV1ApiSpec {

    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다.")
    fun register(request: ProductV1Dto.RegisterRequest): ApiResponse<ProductV1Dto.RegisterResponse>

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 정렬 조건에 따라 조회합니다.")
    fun getProducts(sort: String?): ApiResponse<List<ProductV1Dto.ProductListResponse>>

    @Operation(summary = "상품 상세 조회", description = "상품 상세 정보를 조회합니다. (브랜드 정보 포함)")
    fun getProductDetail(id: Long): ApiResponse<ProductV1Dto.ProductDetailResponse>
}
