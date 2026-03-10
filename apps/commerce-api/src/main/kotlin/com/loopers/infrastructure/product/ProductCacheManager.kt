package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductPageInfo
import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.random.Random

@Component
class ProductCacheManager(
    private val defaultRedisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val masterRedisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    fun getProductDetail(productId: Long): ProductInfo? {
        val key = productDetailKey(productId)
        val value = defaultRedisTemplate.opsForValue().get(key) ?: return null
        return objectMapper.readValue(value, ProductInfo::class.java)
    }

    fun setProductDetail(productId: Long, info: ProductInfo) {
        val key = productDetailKey(productId)
        val value = objectMapper.writeValueAsString(info)
        masterRedisTemplate.opsForValue().set(key, value, detailTtl())
    }

    fun evictProductDetail(productId: Long) {
        masterRedisTemplate.delete(productDetailKey(productId))
    }

    fun getProductList(brandId: Long?, sortType: String, page: Int, size: Int): ProductPageInfo? {
        val key = productListKey(brandId, sortType, page, size)
        val value = defaultRedisTemplate.opsForValue().get(key) ?: return null
        return objectMapper.readValue(value, ProductPageInfo::class.java)
    }

    fun setProductList(brandId: Long?, sortType: String, page: Int, size: Int, info: ProductPageInfo) {
        val key = productListKey(brandId, sortType, page, size)
        val value = objectMapper.writeValueAsString(info)
        masterRedisTemplate.opsForValue().set(key, value, listTtl())
    }

    fun evictProductListByBrandId(brandId: Long) {
        val keys = defaultRedisTemplate.keys("$LIST_PREFIX$brandId:*")
        if (!keys.isNullOrEmpty()) {
            masterRedisTemplate.delete(keys)
        }
    }

    companion object {
        private const val DETAIL_PREFIX = "product:detail:"
        private const val LIST_PREFIX = "product:list:"
        private val DETAIL_BASE_TTL = Duration.ofMinutes(5)
        private val LIST_BASE_TTL = Duration.ofMinutes(3)
        private const val DETAIL_JITTER_SECONDS = 30L
        private const val LIST_JITTER_SECONDS = 15L

        private fun detailTtl(): Duration =
            DETAIL_BASE_TTL.plusSeconds(Random.nextLong(0, DETAIL_JITTER_SECONDS))

        private fun listTtl(): Duration =
            LIST_BASE_TTL.plusSeconds(Random.nextLong(0, LIST_JITTER_SECONDS))

        private fun productDetailKey(productId: Long) = "$DETAIL_PREFIX$productId"

        private fun productListKey(brandId: Long?, sortType: String, page: Int, size: Int): String {
            return "$LIST_PREFIX${brandId ?: "all"}:$sortType:$page:$size"
        }
    }
}
