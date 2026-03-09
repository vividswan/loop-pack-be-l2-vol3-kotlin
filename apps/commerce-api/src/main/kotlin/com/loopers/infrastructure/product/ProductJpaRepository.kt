package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<ProductModel>
    fun findAllByOrderByPriceAsc(): List<ProductModel>
    fun findAllByOrderByLikeCountDesc(): List<ProductModel>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductModel p WHERE p.id = :id")
    fun findByIdWithLock(id: Long): ProductModel?
}
