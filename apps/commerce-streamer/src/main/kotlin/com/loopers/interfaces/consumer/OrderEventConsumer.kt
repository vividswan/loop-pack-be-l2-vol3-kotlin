package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.domain.ranking.RankingRepository
import com.loopers.infrastructure.eventhandled.RedisEventHandledManager
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 이벤트를 소비하여 상품별 판매 메트릭을 집계한다.
 */
@Component
class OrderEventConsumer(
    private val productMetricsRepository: ProductMetricsRepository,
    private val rankingRepository: RankingRepository,
    private val redisEventHandledManager: RedisEventHandledManager,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val ORDER_WEIGHT = 0.7
    }

    @KafkaListener(
        topics = ["\${event-topics.order-events}"],
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = "commerce-streamer-order",
    )
    fun consume(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { record ->
            try {
                processRecord(record)
            } catch (e: Exception) {
                log.error("order 이벤트 처리 실패: offset={}, key={}", record.offset(), record.key(), e)
            }
        }
        acknowledgment.acknowledge()
    }

    @Suppress("UNCHECKED_CAST")
    @Transactional
    fun processRecord(record: ConsumerRecord<Any, Any>) {
        val payload = objectMapper.readValue(record.value().toString(), Map::class.java)
        val eventId = payload["eventId"] as? String ?: return
        val eventType = payload["eventType"] as? String ?: return

        if (eventType != "ORDER_CREATED") return

        if (redisEventHandledManager.isAlreadyHandled(eventId)) {
            log.debug("이미 처리된 이벤트, 건너뜀: eventId={}", eventId)
            return
        }

        val eventPayload = payload["payload"] as? Map<*, *> ?: return
        val items = eventPayload["items"] as? List<Map<String, Any>> ?: return

        items.forEach { item ->
            val productId = (item["productId"] as? Number)?.toLong() ?: return@forEach
            val quantity = (item["quantity"] as? Number)?.toLong() ?: return@forEach
            val price = (item["price"] as? Number)?.toLong() ?: return@forEach

            val metrics = productMetricsRepository.findByProductId(productId)
                ?: productMetricsRepository.save(ProductMetricsModel.create(productId))

            metrics.addOrderMetrics(count = quantity, amount = price * quantity)
            productMetricsRepository.save(metrics)

            rankingRepository.incrementScore(productId, ORDER_WEIGHT * quantity)
        }

        redisEventHandledManager.markAsHandled(eventId)
        log.debug("order 이벤트 처리 완료: eventId={}", eventId)
    }
}
