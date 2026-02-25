package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<ProductModel>
    fun findAllByOrderByPriceAsc(): List<ProductModel>
    fun findAllByOrderByLikeCountDesc(): List<ProductModel>
}
