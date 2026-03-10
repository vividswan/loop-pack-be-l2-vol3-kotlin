package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "product",
    indexes = [
        Index(name = "idx_product_brand_id", columnList = "brand_id"),
        Index(name = "idx_product_brand_like_count", columnList = "brand_id, like_count DESC"),
        Index(name = "idx_product_brand_price", columnList = "brand_id, price ASC"),
        Index(name = "idx_product_brand_created_at", columnList = "brand_id, created_at DESC"),
    ],
)
class ProductModel internal constructor(
    name: String,
    price: Long,
    stock: Int,
    brandId: Long,
) : BaseEntity() {

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "price", nullable = false)
    var price: Long = price
        protected set

    @Column(name = "stock", nullable = false)
    var stock: Int = stock
        protected set

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0
        protected set

    init {
        validateName(name)
        validatePrice(price)
        validateStock(stock)
    }

    override fun guard() {
        validateName(name)
        validatePrice(price)
    }

    fun decreaseStock(quantity: Int) {
        if (this.stock < quantity) {
            throw CoreException(ErrorType.BAD_REQUEST, ProductErrorCode.STOCK_NOT_ENOUGH)
        }
        this.stock -= quantity
    }

    fun increaseLikeCount() {
        this.likeCount++
    }

    fun decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--
        }
    }

    companion object {
        fun create(
            name: String,
            price: Long,
            stock: Int,
            brandId: Long,
        ): ProductModel {
            return ProductModel(
                name = name,
                price = price,
                stock = stock,
                brandId = brandId,
            )
        }

        private fun validateName(name: String) {
            if (name.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, ProductErrorCode.NAME_EMPTY)
            }
        }

        private fun validatePrice(price: Long) {
            if (price < 0) {
                throw CoreException(ErrorType.BAD_REQUEST, ProductErrorCode.PRICE_NEGATIVE)
            }
        }

        private fun validateStock(stock: Int) {
            if (stock < 0) {
                throw CoreException(ErrorType.BAD_REQUEST, ProductErrorCode.STOCK_NEGATIVE)
            }
        }
    }
}
