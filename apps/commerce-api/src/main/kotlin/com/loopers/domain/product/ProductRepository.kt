package com.loopers.domain.product

interface ProductRepository {
    fun save(product: ProductModel): ProductModel
    fun findById(id: Long): ProductModel?
    fun findAllOrderByCreatedAtDesc(): List<ProductModel>
    fun findAllOrderByPriceAsc(): List<ProductModel>
    fun findAllOrderByLikeCountDesc(): List<ProductModel>
}
