package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductSortType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional
    fun register(name: String, price: Long, stock: Int, brandId: Long): ProductInfo {
        brandService.getBrand(brandId)
        val product = productService.register(name, price, stock, brandId)
        return ProductInfo.from(product)
    }

    @Transactional(readOnly = true)
    fun getProducts(sortType: ProductSortType): List<ProductInfo> {
        return productService.getProducts(sortType).map { ProductInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getProducts(brandId: Long?, sortType: ProductSortType, page: Int, size: Int): ProductPageInfo {
        val pageResult = productService.getProducts(brandId, sortType, page, size)
        return ProductPageInfo.from(pageResult)
    }

    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductInfo {
        val product = productService.getProduct(productId)
        val brand = brandService.getBrand(product.brandId)
        return ProductInfo.from(product, brand)
    }
}
