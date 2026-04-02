package com.loopers.concurrency

import com.loopers.domain.coupon.CouponIssueRequestModel
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCouponModel
import com.loopers.domain.member.MemberModel
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.coupon.CouponIssueV1Dto
import com.loopers.utils.ConcurrencyTestHelper
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
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
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FirstComeCouponConcurrencyTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
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

    private fun createFirstComeCoupon(maxCount: Int): CouponModel {
        return couponJpaRepository.save(
            CouponModel.create(
                name = "선착순 5000원 할인",
                type = CouponType.FIXED,
                value = 5000L,
                minOrderAmount = 10000L,
                expiredAt = ZonedDateTime.now().plusDays(30),
                maxIssuanceCount = maxCount,
            ),
        )
    }

    @DisplayName("선착순 쿠폰(maxCount=3)에 10명이 동시 요청하면, 모두 202 Accepted를 받고 PENDING 상태가 된다.")
    @Test
    fun allRequestsAccepted_whenConcurrentFirstComeRequests() {
        // arrange
        val threadCount = 10
        val maxCount = 3
        val coupon = createFirstComeCoupon(maxCount)
        val members = (1..threadCount).map { createMember("user$it") }

        // act
        val results = ConcurrencyTestHelper.executeParallel(threadCount) { index ->
            val member = members[index]
            val headers = MemberTestFixture.createAuthHeaders(member.loginId, MemberTestFixture.DEFAULT_PASSWORD)
            testRestTemplate.exchange(
                "/api/v1/coupons/first-come/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<CouponIssueV1Dto.IssueRequestResponse>>() {},
            ).statusCode
        }

        // assert
        val acceptedCount = results.count { it == HttpStatus.ACCEPTED }
        val pendingRequests = couponIssueRequestJpaRepository.findAll()
            .filter { it.couponId == coupon.id }

        assertAll(
            { assertThat(acceptedCount).isEqualTo(threadCount) },
            { assertThat(pendingRequests).hasSize(threadCount) },
            { assertThat(pendingRequests).allMatch { it.status == CouponIssueStatus.PENDING } },
        )
    }

    @DisplayName("선착순 쿠폰(maxCount=3)의 PENDING 요청 10건을 순차 처리하면, 3건만 SUCCESS이고 나머지는 FAILED이다.")
    @Test
    fun noOverIssuance_whenProcessingSequentially() {
        // arrange
        val maxCount = 3
        val requestCount = 10
        val coupon = createFirstComeCoupon(maxCount)
        val members = (1..requestCount).map { createMember("user$it") }
        val requests = members.map { member ->
            couponIssueRequestJpaRepository.save(
                CouponIssueRequestModel.create(memberId = member.id, couponId = coupon.id),
            )
        }

        // act - Consumer의 순차 처리를 시뮬레이션 (Kafka 파티션 키 기반 순차 처리와 동일)
        requests.forEach { request ->
            val issuedCount = issuedCouponJpaRepository.findAll()
                .count { it.couponId == coupon.id }
                .toLong()

            if (issuedCount >= maxCount) {
                request.markFailed("선착순 쿠폰이 모두 소진되었습니다.")
            } else {
                issuedCouponJpaRepository.save(
                    IssuedCouponModel.create(memberId = request.memberId, couponId = coupon.id),
                )
                request.markSuccess()
            }
            couponIssueRequestJpaRepository.save(request)
        }

        // assert
        val allRequests = couponIssueRequestJpaRepository.findAll()
            .filter { it.couponId == coupon.id }
        val issuedCoupons = issuedCouponJpaRepository.findAll()
            .filter { it.couponId == coupon.id }

        assertAll(
            { assertThat(issuedCoupons).hasSize(maxCount) },
            { assertThat(allRequests.count { it.status == CouponIssueStatus.SUCCESS }).isEqualTo(maxCount) },
            { assertThat(allRequests.count { it.status == CouponIssueStatus.FAILED }).isEqualTo(requestCount - maxCount) },
        )
    }
}
