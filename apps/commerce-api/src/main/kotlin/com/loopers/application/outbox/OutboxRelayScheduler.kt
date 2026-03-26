package com.loopers.application.outbox

import com.loopers.domain.outbox.OutboxRepository
import com.loopers.infrastructure.kafka.KafkaEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 미발행 Outbox 이벤트를 주기적으로 Kafka에 릴레이한다.
 *
 * AFTER_COMMIT에서 즉시 발행을 시도하지만, 네트워크 장애 등으로
 * 발행에 실패한 건을 이 스케줄러가 보완한다.
 *
 * CDC(Debezium) 대비 장점: 운영 복잡도가 낮고 별도 인프라 불필요
 * CDC 대비 단점: 폴링 주기만큼 지연 발생 (3초 이내)
 */
@Component
class OutboxRelayScheduler(
    private val outboxRepository: OutboxRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 3_000)
    fun relayUnpublishedEvents() {
        val events = outboxRepository.findUnpublished(limit = 100)
        if (events.isEmpty()) return

        log.info("미발행 Outbox 이벤트 릴레이 시작: {}건", events.size)
        events.forEach { outbox ->
            kafkaEventPublisher.publishAndMarkCompleted(outbox)
        }
    }
}
