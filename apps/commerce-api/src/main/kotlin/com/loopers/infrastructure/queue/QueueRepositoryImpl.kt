package com.loopers.infrastructure.queue

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.queue.QueueRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class QueueRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : QueueRepository {

    companion object {
        private const val WAITING_QUEUE_KEY = "queue:waiting"
        private const val TOKEN_KEY_PREFIX = "queue:token:"
    }

    override fun addToQueue(memberId: Long): Boolean {
        val score = System.currentTimeMillis().toDouble()
        return redisTemplate.opsForZSet()
            .addIfAbsent(WAITING_QUEUE_KEY, memberId.toString(), score) ?: false
    }

    override fun getPosition(memberId: Long): Long? {
        val rank = redisTemplate.opsForZSet()
            .rank(WAITING_QUEUE_KEY, memberId.toString())
        return rank?.plus(1)
    }

    override fun getTotalWaitingCount(): Long {
        return redisTemplate.opsForZSet()
            .zCard(WAITING_QUEUE_KEY) ?: 0
    }

    override fun popFromQueue(count: Long): List<Long> {
        val members = redisTemplate.opsForZSet()
            .popMin(WAITING_QUEUE_KEY, count)
            ?: return emptyList()

        return members.mapNotNull { it.value?.toLongOrNull() }
    }

    override fun removeFromQueue(memberId: Long) {
        redisTemplate.opsForZSet()
            .remove(WAITING_QUEUE_KEY, memberId.toString())
    }

    override fun issueToken(memberId: Long, token: String, ttlSeconds: Long) {
        redisTemplate.opsForValue()
            .set(TOKEN_KEY_PREFIX + memberId, token, Duration.ofSeconds(ttlSeconds))
    }

    override fun getToken(memberId: Long): String? {
        return redisTemplate.opsForValue()
            .get(TOKEN_KEY_PREFIX + memberId)
    }

    override fun deleteToken(memberId: Long) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + memberId)
    }
}
