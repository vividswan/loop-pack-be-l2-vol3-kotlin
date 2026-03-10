package com.loopers.application.product

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.ProductModel
import org.springframework.data.domain.Page

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

data class ProductPageInfo(
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val items: List<ProductInfo>,
) {
    companion object {
        fun from(pageResult: Page<ProductModel>): ProductPageInfo {
            return ProductPageInfo(
                totalCount = pageResult.totalElements,
                page = pageResult.number,
                size = pageResult.size,
                items = pageResult.content.map { ProductInfo.from(it) },
            )
        }
    }
}
