package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductSortType
import com.loopers.infrastructure.product.ProductLocalCacheManager
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val productLocalCacheManager: ProductLocalCacheManager,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
        try {
            val cached = productLocalCacheManager.getProductList(brandId, sortType, page, size)
            if (cached != null) return cached
        } catch (e: Exception) {
            log.warn("로컬 캐시 조회 실패 (product:list), DB fallback", e)
        }

        val pageResult = productService.getProducts(brandId, sortType, page, size)
        return ProductPageInfo.from(pageResult)
    }

    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductInfo {
        try {
            val cached = productLocalCacheManager.getProductDetail(productId)
            if (cached != null) return cached
        } catch (e: Exception) {
            log.warn("로컬 캐시 조회 실패 (product:detail:$productId), DB fallback", e)
        }

        val product = productService.getProduct(productId)
        val brand = brandService.getBrand(product.brandId)

        eventPublisher.publishEvent(ProductViewedEvent(productId = productId))

        return ProductInfo.from(product, brand)
    }
}
