package com.loopers.application.like

import com.loopers.infrastructure.product.ProductCacheManager
import com.loopers.infrastructure.product.ProductLocalCacheManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LikeCacheEvictEventListener(
    private val productCacheManager: ProductCacheManager,
    private val productLocalCacheManager: ProductLocalCacheManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeCacheEvict(event: LikeCacheEvictEvent) {
        try {
            productCacheManager.evictProductDetail(event.productId)
            productLocalCacheManager.evictProductDetail(event.productId)
        } catch (e: Exception) {
            log.warn("캐시 무효화 실패 (product:detail:${event.productId})", e)
        }
    }
}
