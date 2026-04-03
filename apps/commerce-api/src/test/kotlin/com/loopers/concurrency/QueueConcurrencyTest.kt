package com.loopers.concurrency

import com.loopers.domain.member.MemberModel
import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.queue.QueueService
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.queue.QueueV1Dto
import com.loopers.utils.ConcurrencyTestHelper
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["queue.scheduler.enabled=false"],
)
class QueueConcurrencyTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val queueRepository: QueueRepository,
    private val queueService: QueueService,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun createMember(loginId: String): MemberModel {
        return memberJpaRepository.save(
            MemberModel.create(
                loginId = loginId,
                rawPassword = MemberTestFixture.DEFAULT_PASSWORD,
                passwordEncoder = passwordEncoder,
                name = MemberTestFixture.DEFAULT_NAME,
                birthDate = MemberTestFixture.DEFAULT_BIRTH_DATE,
                email = "$loginId@example.com",
            ),
        )
    }

    @DisplayName("100명이 동시에 대기열에 진입하면, 모두 성공하고 대기열에 100명이 존재한다")
    @Test
    fun allEnterSucceed_whenConcurrentEntry() {
        // arrange
        val threadCount = 100
        val members = (1..threadCount).map { createMember("user$it") }

        // act
        val results = ConcurrencyTestHelper.executeParallel(threadCount) { index ->
            val member = members[index]
            val headers = MemberTestFixture.createAuthHeaders(member.loginId, MemberTestFixture.DEFAULT_PASSWORD)
            testRestTemplate.exchange(
                "/api/v1/queue/enter",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {},
            ).statusCode
        }

        // assert
        val successCount = results.count { it == HttpStatus.CREATED }
        val totalWaiting = queueRepository.getTotalWaitingCount()

        assertAll(
            { assertThat(successCount).isEqualTo(threadCount) },
            { assertThat(totalWaiting).isEqualTo(threadCount.toLong()) },
        )
    }

    @DisplayName("같은 유저가 동시에 대기열에 진입하면, Redis Sorted Set의 중복 방지로 1건만 추가된다")
    @Test
    fun onlyOneAdded_whenSameUserConcurrentEntry() {
        // arrange
        val threadCount = 10
        val memberId = 1L
        val successCount = AtomicInteger(0)
        val executorService = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        // act
        repeat(threadCount) {
            executorService.submit {
                try {
                    startLatch.await()
                    val added = queueRepository.addToQueue(memberId)
                    if (added) successCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }
        startLatch.countDown()
        doneLatch.await()
        executorService.shutdown()

        // assert
        assertThat(successCount.get()).isEqualTo(1)
    }

    @DisplayName("스케줄러가 대기열에서 유저를 꺼내 토큰을 발급하면, READY 상태와 토큰을 반환한다")
    @Test
    fun returnsReadyWithToken_afterProcessQueue() {
        // arrange
        val member = createMember("tokenuser")
        val headers = MemberTestFixture.createAuthHeaders(member.loginId, MemberTestFixture.DEFAULT_PASSWORD)

        testRestTemplate.exchange(
            "/api/v1/queue/enter",
            HttpMethod.POST,
            HttpEntity<Any>(headers),
            object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {},
        )

        // act — 스케줄러 대신 직접 processQueue 호출
        queueService.processQueue()

        // assert
        val positionResponse = testRestTemplate.exchange(
            "/api/v1/queue/position",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {},
        )

        assertAll(
            { assertThat(positionResponse.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(positionResponse.body?.data?.status).isEqualTo("READY") },
            { assertThat(positionResponse.body?.data?.token).isNotNull() },
        )
    }

    @DisplayName("토큰 TTL이 만료되면 순번 조회 시 NOT_IN_QUEUE 상태를 반환한다")
    @Test
    fun returnsNotInQueue_whenTokenExpired() {
        // arrange — 짧은 TTL(1초)로 토큰을 직접 발급
        val member = createMember("expireuser")
        val headers = MemberTestFixture.createAuthHeaders(member.loginId, MemberTestFixture.DEFAULT_PASSWORD)
        queueRepository.issueToken(member.id, "test-token", 1L)

        // act — TTL 만료 대기
        Thread.sleep(1500)

        // assert
        val positionResponse = testRestTemplate.exchange(
            "/api/v1/queue/position",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            object : ParameterizedTypeReference<ApiResponse<QueueV1Dto.PositionResponse>>() {},
        )

        assertAll(
            { assertThat(positionResponse.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(positionResponse.body?.data?.status).isEqualTo("NOT_IN_QUEUE") },
            { assertThat(positionResponse.body?.data?.token).isNull() },
        )
    }
}
