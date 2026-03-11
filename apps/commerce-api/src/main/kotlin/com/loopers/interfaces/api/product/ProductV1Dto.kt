package com.loopers.interfaces.api.product

import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductPageInfo

class ProductV1Dto {
    data class RegisterRequest(
        val name: String,
        val price: Long,
        val stock: Int,
        val brandId: Long,
    )

    data class RegisterResponse(
        val id: Long,
        val name: String,
        val price: Long,
        val stock: Int,
        val brandId: Long,
    ) {
        companion object {
            fun from(info: ProductInfo): RegisterResponse {
                return RegisterResponse(
                    id = info.id,
                    name = info.name,
                    price = info.price,
                    stock = info.stock,
                    brandId = info.brandId,
                )
            }
        }
    }

    data class ProductListResponse(
        val id: Long,
        val name: String,
        val price: Long,
        val stock: Int,
        val likeCount: Int,
        val brandId: Long,
    ) {
        companion object {
            fun from(info: ProductInfo): ProductListResponse {
                return ProductListResponse(
                    id = info.id,
                    name = info.name,
                    price = info.price,
                    stock = info.stock,
                    likeCount = info.likeCount,
                    brandId = info.brandId,
                )
            }
        }
    }

    data class ProductDetailResponse(
        val id: Long,
        val name: String,
        val price: Long,
        val stock: Int,
        val likeCount: Int,
        val brandId: Long,
        val brandName: String?,
    ) {
        companion object {
            fun from(info: ProductInfo): ProductDetailResponse {
                return ProductDetailResponse(
                    id = info.id,
                    name = info.name,
                    price = info.price,
                    stock = info.stock,
                    likeCount = info.likeCount,
                    brandId = info.brandId,
                    brandName = info.brandName,
                )
            }
        }
    }

    data class ProductPageResponse(
        val totalCount: Long,
        val page: Int,
        val size: Int,
        val products: List<ProductListResponse>,
    ) {
        companion object {
            fun from(info: ProductPageInfo): ProductPageResponse {
                return ProductPageResponse(
                    totalCount = info.totalCount,
                    page = info.page,
                    size = info.size,
                    products = info.items.map { ProductListResponse.from(it) },
                )
            }
        }
    }
}
