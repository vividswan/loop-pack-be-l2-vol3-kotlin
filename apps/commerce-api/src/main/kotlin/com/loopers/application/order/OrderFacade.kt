package com.loopers.application.order

import com.loopers.application.event.UserActionEvent
import com.loopers.application.outbox.OutboxPublishEvent
import com.loopers.domain.coupon.CouponErrorCode
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.event.EventTopic
import com.loopers.domain.event.KafkaEventMessage
import com.loopers.domain.order.OrderCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.outbox.OutboxModel
import com.loopers.domain.outbox.OutboxRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun createOrder(memberId: Long, command: OrderCommand.CreateOrder): OrderInfo {
        val order = orderService.createOrder(memberId, command.items)

        if (command.couponId != null) {
            val issuedCoupon = couponService.getIssuedCoupon(command.couponId)

            if (issuedCoupon.memberId != memberId) {
                throw CoreException(ErrorType.FORBIDDEN, CouponErrorCode.OWNER_MISMATCH)
            }

            val coupon = couponService.getCoupon(issuedCoupon.couponId)
            val discountAmount = coupon.calculateDiscount(order.originalPrice)
            order.applyDiscount(discountAmount, coupon.id)
            couponService.useIssuedCoupon(command.couponId)
        }

        // 1. 주문 생성 ApplicationEvent (부가 로직: 유저 행동 로깅)
        eventPublisher.publishEvent(
            OrderCreatedEvent(
                orderId = order.id,
                memberId = memberId,
                totalPrice = order.totalPrice,
                productIds = order.orderItems.map { it.productId },
            ),
        )

        // 2. 유저 행동 로그 이벤트
        eventPublisher.publishEvent(
            UserActionEvent(
                memberId = memberId,
                action = "ORDER",
                targetType = "ORDER",
                targetId = order.id,
                metadata = mapOf("totalPrice" to order.totalPrice),
            ),
        )

        // 3. Kafka Outbox 기록 (시스템 간 전파 — 집계용)
        val kafkaMessage = KafkaEventMessage(
            eventType = "ORDER_CREATED",
            aggregateId = order.id.toString(),
            payload = mapOf(
                "orderId" to order.id,
                "memberId" to memberId,
                "totalPrice" to order.totalPrice,
                "items" to order.orderItems.map {
                    mapOf(
                        "productId" to it.productId,
                        "quantity" to it.quantity,
                        "price" to it.price,
                    )
                },
            ),
        )
        val outbox = outboxRepository.save(
            OutboxModel.create(
                aggregateType = "ORDER",
                aggregateId = order.id.toString(),
                eventType = "ORDER_CREATED",
                payload = objectMapper.writeValueAsString(kafkaMessage),
                topic = EventTopic.ORDER_EVENTS,
            ),
        )
        eventPublisher.publishEvent(OutboxPublishEvent(outbox.id))

        return OrderInfo.from(order)
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderInfo {
        val order = orderService.getOrder(orderId)
        return OrderInfo.from(order)
    }
}
