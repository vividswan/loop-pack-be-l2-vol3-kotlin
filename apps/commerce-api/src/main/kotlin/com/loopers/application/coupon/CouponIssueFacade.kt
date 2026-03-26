package com.loopers.application.coupon

import com.loopers.application.outbox.OutboxPublishEvent
import com.loopers.domain.coupon.CouponErrorCode
import com.loopers.domain.coupon.CouponIssueRequestModel
import com.loopers.domain.coupon.CouponIssueRequestRepository
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.event.EventTopic
import com.loopers.domain.event.KafkaEventMessage
import com.loopers.domain.outbox.OutboxModel
import com.loopers.domain.outbox.OutboxRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 선착순 쿠폰 발급 Facade.
 *
 * API는 발급 요청을 Kafka에 발행만 하고, 실제 발급은 Consumer가 처리한다.
 * 이렇게 하면:
 * - API 응답이 빠르다 (Kafka에 넣기만 하면 됨)
 * - 트래픽이 몰려도 Kafka가 버퍼 역할을 하여 시스템을 보호한다
 * - couponId를 파티션 키로 사용하여 같은 쿠폰의 요청이 하나의 Consumer에서 순차 처리된다
 *   → 동시성 문제가 자연스럽게 해결됨
 *
 * 트레이드오프:
 * - Redis Sorted Set 방식 대비: 진정한 선착순 보장이 어려울 수 있음
 *   (파티션 간 처리 속도 차이, 리트라이로 인한 순서 변경)
 * - 하지만 대규모 트래픽 버퍼링에는 Kafka가 더 적합
 */
@Component
class CouponIssueFacade(
    private val couponService: CouponService,
    private val couponIssueRequestRepository: CouponIssueRequestRepository,
    private val outboxRepository: OutboxRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun requestFirstComeCouponIssue(memberId: Long, couponId: Long): CouponIssueRequestInfo {
        val coupon = couponService.getCoupon(couponId)

        if (coupon.maxIssuanceCount == null) {
            throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.NOT_FIRST_COME_COUPON)
        }

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.EXPIRED)
        }

        // PENDING 상태로 요청 기록
        val request = couponIssueRequestRepository.save(
            CouponIssueRequestModel.create(memberId = memberId, couponId = couponId),
        )

        // Outbox에 Kafka 이벤트 기록
        val kafkaMessage = KafkaEventMessage(
            eventType = "COUPON_ISSUE_REQUESTED",
            aggregateId = couponId.toString(),
            payload = mapOf(
                "requestId" to request.id,
                "memberId" to memberId,
                "couponId" to couponId,
            ),
        )
        val outbox = outboxRepository.save(
            OutboxModel.create(
                aggregateType = "COUPON",
                aggregateId = couponId.toString(),
                eventType = "COUPON_ISSUE_REQUESTED",
                payload = objectMapper.writeValueAsString(kafkaMessage),
                topic = EventTopic.COUPON_ISSUE_REQUESTS,
            ),
        )
        eventPublisher.publishEvent(OutboxPublishEvent(outbox.id))

        return CouponIssueRequestInfo.from(request)
    }

    @Transactional(readOnly = true)
    fun getIssueRequestStatus(requestId: Long): CouponIssueRequestInfo {
        val request = couponIssueRequestRepository.findById(requestId)
            ?: throw CoreException(ErrorType.NOT_FOUND, CouponErrorCode.ISSUE_REQUEST_NOT_FOUND)
        return CouponIssueRequestInfo.from(request)
    }
}
