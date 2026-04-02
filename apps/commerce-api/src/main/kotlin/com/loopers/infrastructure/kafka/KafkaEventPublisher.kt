package com.loopers.infrastructure.kafka

import com.loopers.domain.outbox.OutboxModel
import com.loopers.domain.outbox.OutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val outboxRepository: OutboxRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SEND_TIMEOUT_SECONDS = 5L
    }

    fun publishAndMarkCompleted(outbox: OutboxModel) {
        try {
            kafkaTemplate.send(outbox.topic, outbox.aggregateId, outbox.payload)
                .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            outbox.markPublished()
            outboxRepository.save(outbox)
            log.debug("Kafka 이벤트 발행 완료: topic={}, key={}", outbox.topic, outbox.aggregateId)
        } catch (e: Exception) {
            log.warn(
                "Kafka 이벤트 발행 실패 (스케줄러가 재시도): outboxId={}, topic={}",
                outbox.id,
                outbox.topic,
            )
        }
    }
}
