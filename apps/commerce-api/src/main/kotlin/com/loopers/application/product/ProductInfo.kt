package com.loopers.application.product

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.ProductModel

data class ProductInfo(
    val id: Long,
    val name: String,
    val price: Long,
    val stock: Int,
    val likeCount: Int,
    val brandId: Long,
    val brandName: String?,
) {
    companion object {
        fun from(model: ProductModel): ProductInfo {
            return ProductInfo(
                id = model.id,
                name = model.name,
                price = model.price,
                stock = model.stock,
                likeCount = model.likeCount,
                brandId = model.brandId,
                brandName = null,
            )
        }

        fun from(product: ProductModel, brand: BrandModel): ProductInfo {
            return ProductInfo(
                id = product.id,
                name = product.name,
                price = product.price,
                stock = product.stock,
                likeCount = product.likeCount,
                brandId = product.brandId,
                brandName = brand.name,
            )
        }
    }
}
