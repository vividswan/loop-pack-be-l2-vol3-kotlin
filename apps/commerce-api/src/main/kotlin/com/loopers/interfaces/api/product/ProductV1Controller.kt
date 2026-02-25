package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductFacade
import com.loopers.domain.product.ProductSortType
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class ProductV1Controller(
    private val productFacade: ProductFacade,
) : ProductV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestBody request: ProductV1Dto.RegisterRequest,
    ): ApiResponse<ProductV1Dto.RegisterResponse> {
        return productFacade.register(request.name, request.price, request.stock, request.brandId)
            .let { ProductV1Dto.RegisterResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getProducts(
        @RequestParam(defaultValue = "latest") sort: String?,
    ): ApiResponse<List<ProductV1Dto.ProductListResponse>> {
        val sortType = when (sort?.lowercase()) {
            "price_asc" -> ProductSortType.PRICE_ASC
            "likes_desc" -> ProductSortType.LIKES_DESC
            else -> ProductSortType.LATEST
        }
        return productFacade.getProducts(sortType)
            .map { ProductV1Dto.ProductListResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{id}")
    override fun getProductDetail(
        @PathVariable id: Long,
    ): ApiResponse<ProductV1Dto.ProductDetailResponse> {
        return productFacade.getProductDetail(id)
            .let { ProductV1Dto.ProductDetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
