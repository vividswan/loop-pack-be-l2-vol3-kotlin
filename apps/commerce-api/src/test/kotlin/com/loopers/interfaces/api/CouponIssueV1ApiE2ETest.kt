package com.loopers.interfaces.api

import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.member.MemberModel
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.coupon.CouponIssueV1Dto
import com.loopers.utils.DatabaseCleanUp
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponIssueV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createMember(): MemberModel {
        return memberJpaRepository.save(
            MemberModel.create(
                loginId = MemberTestFixture.DEFAULT_LOGIN_ID,
                rawPassword = MemberTestFixture.DEFAULT_PASSWORD,
                passwordEncoder = passwordEncoder,
                name = MemberTestFixture.DEFAULT_NAME,
                birthDate = MemberTestFixture.DEFAULT_BIRTH_DATE,
                email = MemberTestFixture.DEFAULT_EMAIL,
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

    @DisplayName("POST /api/v1/coupons/first-come/{couponId}/issue")
    @Nested
    inner class RequestIssue {

        @DisplayName("선착순 쿠폰 발급 요청하면, 202 Accepted와 PENDING 상태를 반환한다.")
        @Test
        fun returnsAccepted_whenValidRequest() {
            // arrange
            createMember()
            val coupon = createFirstComeCoupon(100)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<CouponIssueV1Dto.IssueRequestResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/first-come/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED) },
                { assertThat(response.body?.data?.status).isEqualTo("PENDING") },
                { assertThat(response.body?.data?.couponId).isEqualTo(coupon.id) },
            )
        }

        @DisplayName("선착순 쿠폰이 아닌 쿠폰에 발급 요청하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNotFirstComeCoupon() {
            // arrange
            createMember()
            val coupon = couponJpaRepository.save(
                CouponModel.create(
                    name = "일반 쿠폰",
                    type = CouponType.FIXED,
                    value = 3000L,
                    minOrderAmount = 5000L,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<CouponIssueV1Dto.IssueRequestResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/first-come/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("인증 없이 발급 요청하면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val coupon = createFirstComeCoupon(100)

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<CouponIssueV1Dto.IssueRequestResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/first-come/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity(null, null),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
