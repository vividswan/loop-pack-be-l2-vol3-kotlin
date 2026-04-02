package com.loopers.application.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.event.EventTopic
import com.loopers.domain.event.KafkaEventMessage
import com.loopers.domain.outbox.OutboxModel
import com.loopers.domain.outbox.OutboxRepository
import com.loopers.infrastructure.kafka.KafkaEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 상품 조회 이벤트를 처리하여 Kafka로 전파한다.
 *
 * readOnly 트랜잭션에서 발행된 이벤트를 REQUIRES_NEW 트랜잭션으로
 * Outbox에 기록하고 Kafka 발행을 시도한다.
 * 실패 시 OutboxRelayScheduler가 재시도한다.
 */
@Component
class ProductViewedEventHandler(
    private val outboxRepository: OutboxRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("outboxTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleProductViewed(event: ProductViewedEvent) {
        try {
            val kafkaMessage = KafkaEventMessage(
                eventType = "PRODUCT_VIEWED",
                aggregateId = event.productId.toString(),
                payload = mapOf("productId" to event.productId),
            )
            val outbox = outboxRepository.save(
                OutboxModel.create(
                    aggregateType = "PRODUCT",
                    aggregateId = event.productId.toString(),
                    eventType = "PRODUCT_VIEWED",
                    payload = objectMapper.writeValueAsString(kafkaMessage),
                    topic = EventTopic.CATALOG_EVENTS,
                ),
            )
            kafkaEventPublisher.publishAndMarkCompleted(outbox)
        } catch (e: Exception) {
            log.warn("상품 조회 이벤트 처리 실패: productId={}", event.productId, e)
        }
    }
}
