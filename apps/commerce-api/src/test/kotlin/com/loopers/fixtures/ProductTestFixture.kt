package com.loopers.fixtures

import com.loopers.domain.product.ProductModel

object ProductTestFixture {

    const val DEFAULT_NAME = "운동화"
    const val DEFAULT_PRICE = 50000L
    const val DEFAULT_STOCK = 100
    const val DEFAULT_BRAND_ID = 1L

    fun createProduct(
        name: String = DEFAULT_NAME,
        price: Long = DEFAULT_PRICE,
        stock: Int = DEFAULT_STOCK,
        brandId: Long = DEFAULT_BRAND_ID,
    ): ProductModel {
        return ProductModel.create(name = name, price = price, stock = stock, brandId = brandId)
    }
}
