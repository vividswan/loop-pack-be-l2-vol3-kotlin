package com.loopers.infrastructure.ranking

import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest
class RankingCarryOverIntegrationTest @Autowired constructor(
    private val rankingRepository: RankingRepositoryImpl,
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private const val KEY_PREFIX = "rank:all:"
        private const val CARRY_OVER_FLAG_PREFIX = "rank:carryover:"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    private fun todayKey(): String = KEY_PREFIX + LocalDate.now().format(DATE_FORMAT)

    private fun yesterdayKey(): String = KEY_PREFIX + LocalDate.now().minusDays(1).format(DATE_FORMAT)

    private fun carryOverFlag(): String = CARRY_OVER_FLAG_PREFIX + LocalDate.now().format(DATE_FORMAT)

    @DisplayName("carryOverFromYesterday")
    @Nested
    inner class CarryOverFromYesterday {

        @DisplayName("전일 점수의 지정된 비율(10%)을 오늘 키에 ZUNIONSTORE로 반영한다.")
        @Test
        fun carriesOverYesterdayScoresWithWeight() {
            // arrange
            val yesterdayKey = yesterdayKey()
            redisTemplate.opsForZSet().add(yesterdayKey, "1", 100.0)
            redisTemplate.opsForZSet().add(yesterdayKey, "2", 200.0)
            redisTemplate.expire(yesterdayKey, Duration.ofDays(2))

            // act
            rankingRepository.carryOverFromYesterday(0.1)

            // assert
            val todayKey = todayKey()
            val score1 = redisTemplate.opsForZSet().score(todayKey, "1") as Double?
            val score2 = redisTemplate.opsForZSet().score(todayKey, "2") as Double?
            assertAll(
                { assertThat(score1).isEqualTo(10.0) },
                { assertThat(score2).isEqualTo(20.0) },
            )
        }

        @DisplayName("오늘 키에 이미 쌓인 점수는 보존하면서 전일 점수를 합산한다.")
        @Test
        fun preservesTodayScoresAndAddsCarryOver() {
            // arrange
            val yesterdayKey = yesterdayKey()
            redisTemplate.opsForZSet().add(yesterdayKey, "1", 100.0)
            redisTemplate.expire(yesterdayKey, Duration.ofDays(2))

            val todayKey = todayKey()
            redisTemplate.opsForZSet().add(todayKey, "1", 5.0)

            // act
            rankingRepository.carryOverFromYesterday(0.1)

            // assert
            val score = redisTemplate.opsForZSet().score(todayKey, "1") as Double?
            assertThat(score).isEqualTo(15.0) // 기존 5.0 + carry-over 10.0
        }

        @DisplayName("carry-over 완료 후 멱등 플래그를 기록하여, 중복 실행을 방지한다.")
        @Test
        fun setsIdempotencyFlagAfterCarryOver() {
            // arrange
            val yesterdayKey = yesterdayKey()
            redisTemplate.opsForZSet().add(yesterdayKey, "1", 100.0)
            redisTemplate.expire(yesterdayKey, Duration.ofDays(2))

            // act
            rankingRepository.carryOverFromYesterday(0.1)

            // assert
            assertThat(redisTemplate.hasKey(carryOverFlag())).isTrue()
        }

        @DisplayName("멱등 플래그가 있으면, carry-over를 건너뛴다.")
        @Test
        fun skipsCarryOver_whenFlagAlreadyExists() {
            // arrange
            val yesterdayKey = yesterdayKey()
            redisTemplate.opsForZSet().add(yesterdayKey, "1", 100.0)
            redisTemplate.expire(yesterdayKey, Duration.ofDays(2))

            redisTemplate.opsForValue().set(carryOverFlag(), "1", Duration.ofDays(2))

            // act
            rankingRepository.carryOverFromYesterday(0.1)

            // assert — 오늘 키에 점수가 반영되지 않아야 한다
            val todayKey = todayKey()
            val score = redisTemplate.opsForZSet().score(todayKey, "1") as Double?
            assertThat(score).isNull()
        }

        @DisplayName("전일 랭킹 데이터가 없으면, carry-over를 건너뛴다.")
        @Test
        fun skipsCarryOver_whenYesterdayDataNotExists() {
            // act
            rankingRepository.carryOverFromYesterday(0.1)

            // assert
            val todayKey = todayKey()
            val size = redisTemplate.opsForZSet().size(todayKey) ?: 0
            assertThat(size).isZero()
        }
    }
}
