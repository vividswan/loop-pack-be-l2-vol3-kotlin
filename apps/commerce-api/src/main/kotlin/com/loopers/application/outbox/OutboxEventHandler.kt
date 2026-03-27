package com.loopers.application.outbox

import com.loopers.domain.outbox.OutboxRepository
import com.loopers.infrastructure.kafka.KafkaEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OutboxEventHandler(
    private val outboxRepository: OutboxRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 트랜잭션 커밋 이후 Kafka 발행을 시도한다.
     * - AFTER_COMMIT: DB 커밋이 완료된 후에만 실행
     * - @Async: 별도 스레드에서 실행하여 ThreadLocal(트랜잭션 컨텍스트) 격리
     *
     * 실패 시 OutboxRelayScheduler가 미발행 건을 재시도한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventTaskExecutor")
    fun handleOutboxPublish(event: OutboxPublishEvent) {
        val outbox = outboxRepository.findById(event.outboxId) ?: run {
            log.warn("Outbox 이벤트를 찾을 수 없음: outboxId={}", event.outboxId)
            return
        }

        if (outbox.published) return

        kafkaEventPublisher.publishAndMarkCompleted(outbox)
    }
}
