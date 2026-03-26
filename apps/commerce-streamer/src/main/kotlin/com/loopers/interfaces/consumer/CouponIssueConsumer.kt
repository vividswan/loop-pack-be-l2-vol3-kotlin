package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.coupon.StreamerIssuedCouponModel
import com.loopers.infrastructure.coupon.StreamerCouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.StreamerCouponJpaRepository
import com.loopers.infrastructure.coupon.StreamerIssuedCouponJpaRepository
import com.loopers.infrastructure.eventhandled.RedisEventHandledManager
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 선착순 쿠폰 발급 Consumer.
 *
 * couponId가 파티션 키 → 같은 쿠폰의 발급 요청은 동일 파티션 → 하나의 Consumer가 순차 처리
 * → 동시성 문제가 자연스럽게 해결된다.
 *
 * 이 구조의 트레이드오프:
 * - 장점: 별도의 락 없이 동시성 제어 가능, 대규모 트래픽 버퍼링
 * - 단점: 파티션 간 처리 속도 차이로 진정한 '선착순' 보장 어려움
 *   (Redis Sorted Set은 정확한 선착순 가능하지만 트래픽 버퍼링 한계)
 *
 * 인기 쿠폰 = 핫 파티션 가능성:
 * - couponId가 파티션 키이므로 같은 쿠폰 요청은 같은 파티션에 몰림
 * - 하지만 선착순 쿠폰은 시작 직후 빠르게 소진되므로 일시적
 * - 파티션 수를 늘려도 같은 키는 같은 파티션 → 해결 안 됨
 * - 처리량이 진짜 문제가 되면 키를 분할("couponId-suffix")하되 동시성 제어 추가 필요
 */
@Component
class CouponIssueConsumer(
    private val couponRepository: StreamerCouponJpaRepository,
    private val issuedCouponRepository: StreamerIssuedCouponJpaRepository,
    private val issueRequestRepository: StreamerCouponIssueRequestJpaRepository,
    private val redisEventHandledManager: RedisEventHandledManager,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${event-topics.coupon-issue-requests}"],
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = "commerce-streamer-coupon",
    )
    fun consume(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { record ->
            try {
                processRecord(record)
            } catch (e: Exception) {
                log.error("coupon issue 이벤트 처리 실패: offset={}, key={}", record.offset(), record.key(), e)
            }
        }
        acknowledgment.acknowledge()
    }

    @Transactional
    fun processRecord(record: ConsumerRecord<Any, Any>) {
        val payload = objectMapper.readValue(record.value().toString(), Map::class.java)
        val eventId = payload["eventId"] as? String ?: return

        if (redisEventHandledManager.isAlreadyHandled(eventId)) {
            log.debug("이미 처리된 쿠폰 발급 이벤트, 건너뜀: eventId={}", eventId)
            return
        }

        val eventPayload = payload["payload"] as? Map<*, *> ?: return
        val requestId = (eventPayload["requestId"] as? Number)?.toLong() ?: return
        val memberId = (eventPayload["memberId"] as? Number)?.toLong() ?: return
        val couponId = (eventPayload["couponId"] as? Number)?.toLong() ?: return

        val request = issueRequestRepository.findById(requestId).orElse(null) ?: run {
            log.warn("쿠폰 발급 요청을 찾을 수 없음: requestId={}", requestId)
            return
        }

        // 이미 처리된 요청이면 건너뜀
        if (request.status != "PENDING") {
            redisEventHandledManager.markAsHandled(eventId)
            return
        }

        val coupon = couponRepository.findById(couponId).orElse(null) ?: run {
            request.markFailed("쿠폰을 찾을 수 없습니다.")
            issueRequestRepository.save(request)
            redisEventHandledManager.markAsHandled(eventId)
            return
        }

        // 중복 발급 방지
        if (issuedCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            request.markFailed("이미 발급받은 쿠폰입니다.")
            issueRequestRepository.save(request)
            redisEventHandledManager.markAsHandled(eventId)
            return
        }

        // 수량 확인 (같은 파티션 = 같은 Consumer = 순차 처리이므로 count 조회가 안전)
        val maxCount = coupon.maxIssuanceCount
        if (maxCount != null) {
            val issuedCount = issuedCouponRepository.countByCouponId(couponId)
            if (issuedCount >= maxCount) {
                request.markFailed("선착순 쿠폰이 모두 소진되었습니다.")
                issueRequestRepository.save(request)
                redisEventHandledManager.markAsHandled(eventId)
                return
            }
        }

        // 쿠폰 발급
        issuedCouponRepository.save(StreamerIssuedCouponModel.create(memberId, couponId))
        request.markSuccess()
        issueRequestRepository.save(request)
        redisEventHandledManager.markAsHandled(eventId)

        log.info("선착순 쿠폰 발급 완료: couponId={}, memberId={}", couponId, memberId)
    }
}
