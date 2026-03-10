package com.loopers.domain.product

import org.springframework.data.domain.Page

interface ProductRepository {
    fun save(product: ProductModel): ProductModel
    fun findById(id: Long): ProductModel?
    fun findByIdWithLock(id: Long): ProductModel?
    fun findAllOrderByCreatedAtDesc(): List<ProductModel>
    fun findAllOrderByPriceAsc(): List<ProductModel>
    fun findAllOrderByLikeCountDesc(): List<ProductModel>
    fun findProducts(brandId: Long?, sortType: ProductSortType, page: Int, size: Int): Page<ProductModel>
}
