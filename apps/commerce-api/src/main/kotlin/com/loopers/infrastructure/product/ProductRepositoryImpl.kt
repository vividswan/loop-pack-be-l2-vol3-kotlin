package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: ProductModel): ProductModel {
        return productJpaRepository.save(product)
    }

    override fun findById(id: Long): ProductModel? {
        return productJpaRepository.findById(id).orElse(null)
    }

    override fun findAllOrderByCreatedAtDesc(): List<ProductModel> {
        return productJpaRepository.findAllByOrderByCreatedAtDesc()
    }

    override fun findAllOrderByPriceAsc(): List<ProductModel> {
        return productJpaRepository.findAllByOrderByPriceAsc()
    }

    override fun findAllOrderByLikeCountDesc(): List<ProductModel> {
        return productJpaRepository.findAllByOrderByLikeCountDesc()
    }
}
