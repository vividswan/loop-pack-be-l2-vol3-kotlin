package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplateMaster: RedisTemplate<String, String>,
) : RankingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getTopRankings(limit: Long): List<RankingEntry> {
        val key = todayKey()
        val results = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, 0, limit - 1)
            ?: return emptyList()

        return results.mapIndexed { index, typedTuple ->
            RankingEntry(
                productId = typedTuple.value?.toLong() ?: 0L,
                score = typedTuple.score ?: 0.0,
                rank = index + 1L,
            )
        }
    }

    override fun getProductRankEntry(productId: Long): RankingEntry? {
        val key = todayKey()
        val member = productId.toString()
        val rank = redisTemplate.opsForZSet().reverseRank(key, member) ?: return null
        val score = redisTemplate.opsForZSet().score(key, member) ?: return null
        return RankingEntry(productId = productId, score = score, rank = rank + 1)
    }

    override fun carryOverFromYesterday(weight: Double) {
        val today = LocalDate.now()
        val todayKey = KEY_PREFIX + today.format(DATE_FORMAT)
        val yesterdayKey = KEY_PREFIX + today.minusDays(1).format(DATE_FORMAT)
        val carryOverFlag = CARRY_OVER_FLAG_PREFIX + today.format(DATE_FORMAT)

        if (redisTemplate.hasKey(carryOverFlag) == true) {
            log.debug("오늘 carry-over 이미 완료됨")
            return
        }

        if (redisTemplate.hasKey(yesterdayKey) != true) {
            log.info("전일 랭킹 데이터 없음, carry-over 건너뜀: {}", yesterdayKey)
            return
        }

        redisTemplateMaster.opsForZSet().unionAndStore(
            todayKey,
            listOf(yesterdayKey),
            todayKey,
            Aggregate.SUM,
            Weights.of(1.0, weight),
        )

        redisTemplateMaster.expire(todayKey, KEY_TTL)
        redisTemplateMaster.opsForValue().set(carryOverFlag, "1", KEY_TTL)

        log.info("carry-over 완료: {} → {} (weight={})", yesterdayKey, todayKey, weight)
    }

    companion object {
        private const val KEY_PREFIX = "rank:all:"
        private const val CARRY_OVER_FLAG_PREFIX = "rank:carryover:"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val KEY_TTL = Duration.ofDays(2)

        fun todayKey(): String {
            return KEY_PREFIX + LocalDate.now().format(DATE_FORMAT)
        }
    }
}
