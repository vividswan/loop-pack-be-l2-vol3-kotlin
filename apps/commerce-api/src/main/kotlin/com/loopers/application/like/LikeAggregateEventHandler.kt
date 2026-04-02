package com.loopers.application.like

import com.loopers.domain.product.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 좋아요 집계를 비동기로 처리한다.
 *
 * 좋아요 저장(핵심)과 좋아요 수 집계(부가)를 분리하여
 * 집계 실패가 좋아요 자체의 성공에 영향을 주지 않도록 한다.
 * (Eventual Consistency)
 *
 * @Async: 별도 스레드에서 실행하여 기존 트랜잭션의 ThreadLocal 컨텍스트 격리
 * REQUIRES_NEW: 새로운 트랜잭션에서 집계 업데이트
 */
@Component
class LikeAggregateEventHandler(
    private val productRepository: ProductRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleLikeAggregate(event: LikeAggregateEvent) {
        try {
            val product = productRepository.findByIdWithLock(event.productId) ?: run {
                log.warn("집계 대상 상품을 찾을 수 없음: productId={}", event.productId)
                return
            }
            if (event.increment) {
                product.increaseLikeCount()
            } else {
                product.decreaseLikeCount()
            }
        } catch (e: Exception) {
            log.warn("좋아요 집계 실패 (eventual consistency): productId={}", event.productId, e)
        }
    }
}
