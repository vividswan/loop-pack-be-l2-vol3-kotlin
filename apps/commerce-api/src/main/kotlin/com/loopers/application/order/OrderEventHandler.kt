package com.loopers.application.order

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 주문 생성 후 부가 로직을 비동기로 처리한다.
 *
 * 핵심 트랜잭션(주문 생성 + 재고 차감)과 부가 로직(로깅, 알림)을 분리하여
 * 부가 로직 실패가 주문 성공에 영향을 주지 않도록 한다.
 */
@Component
class OrderEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventTaskExecutor")
    fun handleOrderCreated(event: OrderCreatedEvent) {
        log.info(
            "[유저 행동 로그] 주문 생성 - memberId={}, orderId={}, totalPrice={}, products={}",
            event.memberId,
            event.orderId,
            event.totalPrice,
            event.productIds,
        )
    }
}
