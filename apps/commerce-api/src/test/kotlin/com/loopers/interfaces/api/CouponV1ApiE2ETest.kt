package com.loopers.interfaces.api

import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.member.MemberModel
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.coupon.CouponV1Dto
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
class CouponV1ApiE2ETest @Autowired constructor(
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

    private fun createCoupon(
        name: String = "5000원 할인",
        type: CouponType = CouponType.FIXED,
        value: Long = 5000L,
        minOrderAmount: Long = 10000L,
    ): CouponModel {
        return couponJpaRepository.save(
            CouponModel.create(
                name = name,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    @Nested
    inner class IssueCoupon {

        @DisplayName("유효한 요청으로 쿠폰 발급하면, 201 Created 응답을 받는다.")
        @Test
        fun returnsCreated_whenValidRequest() {
            // arrange
            createMember()
            val coupon = createCoupon()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.couponId).isEqualTo(coupon.id) },
                { assertThat(response.body?.data?.status).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("이미 발급받은 쿠폰을 다시 발급 요청하면, 409 Conflict 응답을 받는다.")
        @Test
        fun returnsConflict_whenAlreadyIssued() {
            // arrange
            createMember()
            val coupon = createCoupon()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // 첫 번째 발급
            testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {},
            )

            // act - 두 번째 발급
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("존재하지 않는 쿠폰을 발급 요청하면, 404 Not Found 응답을 받는다.")
        @Test
        fun returnsNotFound_whenCouponDoesNotExist() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/999/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/users/me/coupons")
    @Nested
    inner class GetMyCoupons {

        @DisplayName("내 쿠폰 목록을 조회하면, 200 OK와 발급 쿠폰 목록을 반환한다.")
        @Test
        fun returnsOkWithCouponList() {
            // arrange
            createMember()
            val coupon1 = createCoupon(name = "쿠폰1")
            val coupon2 = createCoupon(name = "쿠폰2")
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // 쿠폰 발급
            testRestTemplate.exchange(
                "/api/v1/coupons/${coupon1.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {},
            )
            testRestTemplate.exchange(
                "/api/v1/coupons/${coupon2.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {},
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<CouponV1Dto.IssuedCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(2) },
            )
        }
    }
}
