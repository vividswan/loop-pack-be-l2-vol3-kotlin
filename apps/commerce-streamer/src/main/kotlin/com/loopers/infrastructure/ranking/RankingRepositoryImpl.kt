package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.RankingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun incrementScore(productId: Long, score: Double) {
        try {
            val key = todayKey()
            redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
            ensureTtl(key)
        } catch (e: Exception) {
            log.warn("랭킹 점수 갱신 실패: productId={}, score={}", productId, score, e)
        }
    }

    private fun ensureTtl(key: String) {
        val ttl = redisTemplate.getExpire(key) ?: -1
        if (ttl == -1L) {
            redisTemplate.expire(key, KEY_TTL)
        }
    }

    companion object {
        private const val KEY_PREFIX = "rank:all:"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val KEY_TTL = Duration.ofDays(2)

        fun todayKey(): String {
            return KEY_PREFIX + LocalDate.now().format(DATE_FORMAT)
        }
    }
}
