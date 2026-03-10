package com.loopers.infrastructure.product

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductPageInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductSortType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Optional

@Component
class ProductLocalCacheManager(
    private val productCacheManager: ProductCacheManager,
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val detailCache: LoadingCache<Long, Optional<ProductInfo>> =
        Caffeine.newBuilder()
            .refreshAfterWrite(Duration.ofSeconds(2))
            .expireAfterWrite(Duration.ofSeconds(10))
            .build { productId -> loadProductDetail(productId) }

    private val listCache: LoadingCache<String, Optional<ProductPageInfo>> =
        Caffeine.newBuilder()
            .refreshAfterWrite(Duration.ofSeconds(2))
            .expireAfterWrite(Duration.ofSeconds(10))
            .build { key -> loadProductList(key) }

    fun getProductDetail(productId: Long): ProductInfo? {
        return detailCache.get(productId)?.orElse(null)
    }

    fun getProductList(
        brandId: Long?,
        sortType: ProductSortType,
        page: Int,
        size: Int,
    ): ProductPageInfo? {
        val key = listCacheKey(brandId, sortType.name, page, size)
        return listCache.get(key)?.orElse(null)
    }

    fun evictProductDetail(productId: Long) {
        detailCache.invalidate(productId)
    }

    fun evictAll() {
        detailCache.invalidateAll()
        listCache.invalidateAll()
    }

    private fun loadProductDetail(productId: Long): Optional<ProductInfo> {
        try {
            val cached = productCacheManager.getProductDetail(productId)
            if (cached != null) return Optional.of(cached)
        } catch (e: Exception) {
            log.warn("Redis 캐시 조회 실패 (product:detail:$productId)", e)
        }

        return try {
            val product = productService.getProduct(productId)
            val brand = brandService.getBrand(product.brandId)
            val info = ProductInfo.from(product, brand)
            try {
                productCacheManager.setProductDetail(productId, info)
            } catch (e: Exception) {
                log.warn("Redis 캐시 저장 실패 (product:detail:$productId)", e)
            }
            Optional.of(info)
        } catch (e: Exception) {
            log.warn("상품 조회 실패 (productId=$productId)", e)
            Optional.empty()
        }
    }

    private fun loadProductList(key: String): Optional<ProductPageInfo> {
        val (brandId, sortType, page, size) = parseListCacheKey(key)

        try {
            val cached = productCacheManager.getProductList(brandId, sortType, page, size)
            if (cached != null) return Optional.of(cached)
        } catch (e: Exception) {
            log.warn("Redis 캐시 조회 실패 (product:list:$key)", e)
        }

        return try {
            val pageResult = productService.getProducts(brandId, ProductSortType.valueOf(sortType), page, size)
            val info = ProductPageInfo.from(pageResult)
            try {
                productCacheManager.setProductList(brandId, sortType, page, size, info)
            } catch (e: Exception) {
                log.warn("Redis 캐시 저장 실패 (product:list:$key)", e)
            }
            Optional.of(info)
        } catch (e: Exception) {
            log.warn("상품 목록 조회 실패 (key=$key)", e)
            Optional.empty()
        }
    }

    companion object {
        private fun listCacheKey(brandId: Long?, sortType: String, page: Int, size: Int): String {
            return "${brandId ?: "all"}:$sortType:$page:$size"
        }

        private fun parseListCacheKey(key: String): ListCacheKeyParts {
            val parts = key.split(":")
            val brandId = if (parts[0] == "all") null else parts[0].toLong()
            return ListCacheKeyParts(brandId, parts[1], parts[2].toInt(), parts[3].toInt())
        }
    }

    private data class ListCacheKeyParts(
        val brandId: Long?,
        val sortType: String,
        val page: Int,
        val size: Int,
    )
}
