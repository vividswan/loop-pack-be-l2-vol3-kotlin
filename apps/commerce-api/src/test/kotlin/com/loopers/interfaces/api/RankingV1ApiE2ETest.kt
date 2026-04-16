package com.loopers.interfaces.api

import com.loopers.interfaces.api.ranking.RankingV1Dto
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private const val RANKINGS_ENDPOINT = "/api/v1/rankings"
        private const val PRODUCT_RANK_ENDPOINT = "/api/v1/rankings/products"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun todayKey(): String {
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "rank:all:$today"
    }

    private fun seedRankingData(vararg entries: Pair<Long, Double>) {
        val key = todayKey()
        entries.forEach { (productId, score) ->
            redisTemplate.opsForZSet().add(key, productId.toString(), score)
        }
    }

    @DisplayName("GET /api/v1/rankings")
    @Nested
    inner class GetTopRankings {

        @DisplayName("Redis ZSET에 점수가 있으면, 점수 내림차순으로 Top-N 랭킹을 반환한다.")
        @Test
        fun returnsRankingsSortedByScoreDesc() {
            // arrange
            seedRankingData(
                1L to 10.0,
                2L to 30.0,
                3L to 20.0,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.TopRankingsResponse>>() {}
            val response = testRestTemplate.exchange(
                RANKINGS_ENDPOINT,
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            val rankings = response.body?.data?.rankings!!
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(rankings).hasSize(3) },
                { assertThat(rankings[0].productId).isEqualTo(2L) },
                { assertThat(rankings[0].score).isEqualTo(30.0) },
                { assertThat(rankings[0].rank).isEqualTo(1L) },
                { assertThat(rankings[1].productId).isEqualTo(3L) },
                { assertThat(rankings[1].rank).isEqualTo(2L) },
                { assertThat(rankings[2].productId).isEqualTo(1L) },
                { assertThat(rankings[2].rank).isEqualTo(3L) },
            )
        }

        @DisplayName("limit 파라미터로 반환 개수를 제한할 수 있다.")
        @Test
        fun returnsLimitedRankings_whenLimitIsProvided() {
            // arrange
            seedRankingData(
                1L to 10.0,
                2L to 30.0,
                3L to 20.0,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.TopRankingsResponse>>() {}
            val response = testRestTemplate.exchange(
                "$RANKINGS_ENDPOINT?limit=2",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            val rankings = response.body?.data?.rankings!!
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(rankings).hasSize(2) },
                { assertThat(rankings[0].productId).isEqualTo(2L) },
                { assertThat(rankings[1].productId).isEqualTo(3L) },
            )
        }

        @DisplayName("랭킹 데이터가 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoRankingData() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.TopRankingsResponse>>() {}
            val response = testRestTemplate.exchange(
                RANKINGS_ENDPOINT,
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.rankings).isEmpty() },
            )
        }
    }

    @DisplayName("GET /api/v1/rankings/products/{productId}")
    @Nested
    inner class GetProductRank {

        @DisplayName("ZSET에 존재하는 상품의 순위와 점수를 반환한다.")
        @Test
        fun returnsProductRank_whenExists() {
            // arrange
            seedRankingData(
                1L to 10.0,
                2L to 30.0,
                3L to 20.0,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.ProductRankResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_RANK_ENDPOINT/3",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.productId).isEqualTo(3L) },
                { assertThat(response.body?.data?.score).isEqualTo(20.0) },
                { assertThat(response.body?.data?.rank).isEqualTo(2L) },
            )
        }

        @DisplayName("ZSET에 존재하지 않는 상품을 조회하면, 404 Not Found 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductNotRanked() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.ProductRankResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_RANK_ENDPOINT/999",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
