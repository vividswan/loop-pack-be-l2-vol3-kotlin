package com.loopers.application.queue

import com.loopers.domain.queue.QueueService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 대기열에서 유저를 꺼내 입장 토큰을 발급하는 스케줄러.
 *
 * 처리량 설계 기준:
 * - DB 커넥션 풀: 50
 * - 주문 1건 평균 처리 시간: 200ms
 * - 이론적 최대 TPS: 50 / 0.2 = 250 TPS
 * - 안전 마진 70%: 175 TPS
 * - 스케줄러 주기: 100ms
 * - 배치 크기: 175 / 10 = ~18명
 *
 * Thundering Herd 완화:
 * 1초에 175명을 한 번에 발급하지 않고, 100ms마다 18명씩 나누어 발급하여
 * 주문 API 진입 시점을 분산시킨다.
 */
@Component
@ConditionalOnProperty(name = ["queue.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class QueueScheduler(
    private val queueService: QueueService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 100)
    fun processQueue() {
        val processedMemberIds = queueService.processQueue()
        if (processedMemberIds.isNotEmpty()) {
            log.info("대기열 처리: {}명 토큰 발급", processedMemberIds.size)
        }
    }
}
