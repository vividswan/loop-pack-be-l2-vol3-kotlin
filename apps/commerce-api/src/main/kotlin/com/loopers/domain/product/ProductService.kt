package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {
    fun register(name: String, price: Long, stock: Int, brandId: Long): ProductModel {
        val product = ProductModel.create(name = name, price = price, stock = stock, brandId = brandId)
        return productRepository.save(product)
    }

    fun getProduct(id: Long): ProductModel {
        return productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, ProductErrorCode.NOT_FOUND)
    }

    fun getProducts(sortType: ProductSortType): List<ProductModel> {
        return when (sortType) {
            ProductSortType.LATEST -> productRepository.findAllOrderByCreatedAtDesc()
            ProductSortType.PRICE_ASC -> productRepository.findAllOrderByPriceAsc()
            ProductSortType.LIKES_DESC -> productRepository.findAllOrderByLikeCountDesc()
        }
    }
}
