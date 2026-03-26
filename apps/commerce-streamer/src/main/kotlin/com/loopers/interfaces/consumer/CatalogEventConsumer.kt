package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.infrastructure.eventhandled.RedisEventHandledManager
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 상품/좋아요 이벤트를 소비하여 product_metrics에 집계한다.
 *
 * 파티션 키가 productId이므로 같은 상품의 이벤트는 항상 같은 파티션에 적재되어
 * 하나의 Consumer가 순차 처리한다. → 순서 보장
 *
 * 멱등 처리: Redis event_handled로 중복 이벤트를 건너뛴다.
 * version/updatedAt 기준으로 최신 이벤트만 반영한다.
 */
@Component
class CatalogEventConsumer(
    private val productMetricsRepository: ProductMetricsRepository,
    private val redisEventHandledManager: RedisEventHandledManager,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${event-topics.catalog-events}"],
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = "commerce-streamer-catalog",
    )
    fun consume(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { record ->
            try {
                processRecord(record)
            } catch (e: Exception) {
                log.error("catalog 이벤트 처리 실패: offset={}, key={}", record.offset(), record.key(), e)
            }
        }
        acknowledgment.acknowledge()
    }

    @Transactional
    fun processRecord(record: ConsumerRecord<Any, Any>) {
        val payload = objectMapper.readValue(record.value().toString(), Map::class.java)
        val eventId = payload["eventId"] as? String ?: return
        val eventType = payload["eventType"] as? String ?: return

        if (redisEventHandledManager.isAlreadyHandled(eventId)) {
            log.debug("이미 처리된 이벤트, 건너뜀: eventId={}", eventId)
            return
        }

        val eventPayload = payload["payload"] as? Map<*, *> ?: return
        val productId = (eventPayload["productId"] as? Number)?.toLong() ?: return

        val metrics = productMetricsRepository.findByProductId(productId)
            ?: productMetricsRepository.save(ProductMetricsModel.create(productId))

        when (eventType) {
            "PRODUCT_LIKED" -> metrics.incrementLikeCount()
            "PRODUCT_UNLIKED" -> metrics.decrementLikeCount()
            "PRODUCT_VIEWED" -> metrics.incrementViewCount()
        }

        productMetricsRepository.save(metrics)
        redisEventHandledManager.markAsHandled(eventId)

        log.debug("catalog 이벤트 처리 완료: eventType={}, productId={}", eventType, productId)
    }
}
