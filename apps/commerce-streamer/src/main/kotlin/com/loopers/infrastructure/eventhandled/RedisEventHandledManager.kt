package com.loopers.infrastructure.eventhandled

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Redis 기반 멱등 처리 관리자.
 *
 * DB 대비 장점:
 * - 수억 건/일 규모에서도 키 기반 TTL로 자동 정리 (DB는 파티션/샤딩 필요)
 * - Redis 클러스터의 쓰기 분산으로 높은 처리량 확보
 *
 * DB 대비 단점:
 * - Redis 장애 시 event_handled 기록 유실 → 중복 처리 가능
 * - 다만 Consumer 로직이 멱등하게 설계되어 있으면 중복 처리가 문제되지 않음
 */
@Component
class RedisEventHandledManager(
    private val redisTemplate: StringRedisTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "event:handled:"
        private val TTL = Duration.ofDays(7)
    }

    fun isAlreadyHandled(eventId: String): Boolean {
        return try {
            redisTemplate.hasKey("$KEY_PREFIX$eventId")
        } catch (e: Exception) {
            log.warn("Redis 멱등 조회 실패, 처리 진행: eventId={}", eventId, e)
            false
        }
    }

    fun markAsHandled(eventId: String) {
        try {
            redisTemplate.opsForValue().set("$KEY_PREFIX$eventId", "1", TTL)
        } catch (e: Exception) {
            log.warn("Redis 멱등 기록 실패: eventId={}", eventId, e)
        }
    }
}
