package com.loopers.application.like

import com.loopers.application.event.UserActionEvent
import com.loopers.application.outbox.OutboxPublishEvent
import com.loopers.domain.event.EventTopic
import com.loopers.domain.event.KafkaEventMessage
import com.loopers.domain.like.LikeService
import com.loopers.domain.outbox.OutboxModel
import com.loopers.domain.outbox.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun like(memberId: Long, productId: Long): LikeInfo {
        val like = likeService.like(memberId, productId)

        // 1. 좋아요 집계 이벤트 (ApplicationEvent - 앱 내부 비동기)
        eventPublisher.publishEvent(LikeAggregateEvent(productId = productId, increment = true))

        // 2. 캐시 무효화 이벤트
        eventPublisher.publishEvent(LikeCacheEvictEvent(productId))

        // 3. 유저 행동 로그 이벤트
        eventPublisher.publishEvent(
            UserActionEvent(
                memberId = memberId,
                action = "LIKE",
                targetType = "PRODUCT",
                targetId = productId,
            ),
        )

        // 4. Kafka Outbox 기록 (시스템 간 전파)
        val kafkaMessage = KafkaEventMessage(
            eventType = "PRODUCT_LIKED",
            aggregateId = productId.toString(),
            payload = mapOf("productId" to productId, "memberId" to memberId),
        )
        val outbox = outboxRepository.save(
            OutboxModel.create(
                aggregateType = "PRODUCT",
                aggregateId = productId.toString(),
                eventType = "PRODUCT_LIKED",
                payload = objectMapper.writeValueAsString(kafkaMessage),
                topic = EventTopic.CATALOG_EVENTS,
            ),
        )
        eventPublisher.publishEvent(OutboxPublishEvent(outbox.id))

        return LikeInfo.from(like)
    }

    @Transactional
    fun unlike(memberId: Long, productId: Long) {
        likeService.unlike(memberId, productId)

        eventPublisher.publishEvent(LikeAggregateEvent(productId = productId, increment = false))
        eventPublisher.publishEvent(LikeCacheEvictEvent(productId))

        val kafkaMessage = KafkaEventMessage(
            eventType = "PRODUCT_UNLIKED",
            aggregateId = productId.toString(),
            payload = mapOf("productId" to productId, "memberId" to memberId),
        )
        val outbox = outboxRepository.save(
            OutboxModel.create(
                aggregateType = "PRODUCT",
                aggregateId = productId.toString(),
                eventType = "PRODUCT_UNLIKED",
                payload = objectMapper.writeValueAsString(kafkaMessage),
                topic = EventTopic.CATALOG_EVENTS,
            ),
        )
        eventPublisher.publishEvent(OutboxPublishEvent(outbox.id))
    }
}
