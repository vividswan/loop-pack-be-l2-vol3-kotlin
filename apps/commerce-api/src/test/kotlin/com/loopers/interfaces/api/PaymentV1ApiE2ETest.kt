package com.loopers.interfaces.api

import com.loopers.domain.member.MemberModel
import com.loopers.domain.payment.PaymentStatus
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.infrastructure.payment.PaymentJpaRepository
import com.loopers.infrastructure.payment.pg.PgClient
import com.loopers.infrastructure.payment.pg.PgCallbackPayload
import com.loopers.infrastructure.payment.pg.PgOrderPaymentResponse
import com.loopers.infrastructure.payment.pg.PgPaymentResponse
import com.loopers.infrastructure.payment.pg.PgTransactionItem
import com.loopers.interfaces.api.payment.PaymentV1Dto
import com.loopers.utils.DatabaseCleanUp
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val paymentJpaRepository: PaymentJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
    private val jdbcTemplate: JdbcTemplate,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
) {
    @MockitoBean
    private lateinit var pgClient: PgClient

    companion object {
        private const val PAYMENT_ENDPOINT = "/api/v1/payments"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        circuitBreakerRegistry.circuitBreaker("pg").reset()
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

    @DisplayName("POST /api/v1/payments")
    @Nested
    inner class Pay {

        @DisplayName("PG 요청이 성공하면 201 Created와 PENDING 상태를 반환한다.")
        @Test
        fun returnsCreatedWithPending_whenPgRequestSucceeds() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )
            whenever(pgClient.requestPayment(any(), any())).thenReturn(
                PgPaymentResponse(transactionKey = "20250320:TR:abc123", status = "PENDING"),
            )

            val request = PaymentV1Dto.PayRequest(orderId = 1L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456", amount = 10000L)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.status).isEqualTo(PaymentStatus.PENDING.name) },
                { assertThat(response.body?.data?.amount).isEqualTo(10000L) },
            )
        }

        @DisplayName("PG가 즉시 FAILED를 반환하면 생성된 결제 상태가 FAILED이다.")
        @Test
        fun returnsCreatedWithFailed_whenPgImmediatelyFails() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )
            whenever(pgClient.requestPayment(any(), any())).thenReturn(
                PgPaymentResponse(transactionKey = null, status = "FAILED", reason = "잘못된 카드입니다."),
            )

            val request = PaymentV1Dto.PayRequest(orderId = 1L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456", amount = 10000L)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.status).isEqualTo(PaymentStatus.FAILED.name) },
                { assertThat(response.body?.data?.failureReason).isEqualTo("잘못된 카드입니다.") },
            )
        }

        @DisplayName("인증 없이 결제 요청하면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            val request = PaymentV1Dto.PayRequest(orderId = 1L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456", amount = 10000L)

            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("서킷 브레이커 오픈 상태에서 결제 요청하면, 400 Bad Request와 PG_UNAVAILABLE 에러를 반환한다.")
        @Test
        fun returnsPgUnavailable_whenCircuitBreakerIsOpen() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )
            // CB를 직접 OPEN 상태로 전환 — PgClientAdapter의 @CircuitBreaker AOP가 실제로 차단하는지 검증
            circuitBreakerRegistry.circuitBreaker("pg").transitionToOpenState()

            val request = PaymentV1Dto.PayRequest(orderId = 1L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456", amount = 10000L)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert — CB가 실제로 열려 있어 PG 호출 자체가 차단되며,
            //          결제 레코드는 즉시 FAILED로 확정되어야 한다
            val savedPayment = paymentJpaRepository.findAll().first()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(savedPayment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(savedPayment.failureReason).isEqualTo("결제 시스템 일시 이용 불가") },
            )
        }
    }

    @DisplayName("POST /api/v1/payments/callback")
    @Nested
    inner class Callback {

        @DisplayName("SUCCESS 콜백을 수신하면 결제 상태가 SUCCESS로 변경된다.")
        @Test
        fun updatesStatusToSuccess_whenCallbackIsSuccess() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )
            whenever(pgClient.requestPayment(any(), any())).thenReturn(
                PgPaymentResponse(transactionKey = "20250320:TR:abc123", status = "PENDING"),
            )
            val payRequest = PaymentV1Dto.PayRequest(orderId = 1L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456", amount = 10000L)
            testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(payRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {},
            )

            val payment = paymentJpaRepository.findAll().first()
            val callbackPayload = PgCallbackPayload(
                transactionKey = "20250320:TR:abc123",
                orderId = payment.pgOrderId,
                status = "SUCCESS",
                reason = null,
            )

            // act
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(callbackPayload),
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            // assert
            val updatedPayment = paymentJpaRepository.findById(payment.id).get()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS) },
                { assertThat(updatedPayment.pgTransactionKey).isEqualTo("20250320:TR:abc123") },
            )
        }

        @DisplayName("이미 처리된 결제에 콜백이 오면, 400 Bad Request를 반환한다.")
        @Test
        fun returnsBadRequest_whenPaymentAlreadyProcessed() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )
            whenever(pgClient.requestPayment(any(), any())).thenReturn(
                PgPaymentResponse(transactionKey = "20250320:TR:abc123", status = "PENDING"),
            )
            val payRequest = PaymentV1Dto.PayRequest(orderId = 1L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456", amount = 10000L)
            testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(payRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {},
            )

            val payment = paymentJpaRepository.findAll().first()
            val payload = PgCallbackPayload(
                transactionKey = "20250320:TR:abc123",
                orderId = payment.pgOrderId,
                status = "SUCCESS",
            )

            // 첫 번째 콜백 처리
            testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(payload),
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            // act — 두 번째 콜백 (중복)
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/callback",
                HttpMethod.POST,
                HttpEntity(payload),
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/payments/{id}")
    @Nested
    inner class GetPayment {

        @DisplayName("결제 정보를 조회하면, 200 OK와 결제 정보를 반환한다.")
        @Test
        fun returnsOkWithPaymentInfo() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )
            whenever(pgClient.requestPayment(any(), any())).thenReturn(
                PgPaymentResponse(transactionKey = "20250320:TR:abc123", status = "PENDING"),
            )
            val payRequest = PaymentV1Dto.PayRequest(orderId = 1L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456", amount = 10000L)
            val createResponse = testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(payRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {},
            )
            val paymentId = createResponse.body?.data?.id

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/$paymentId",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(paymentId) },
                { assertThat(response.body?.data?.status).isEqualTo(PaymentStatus.PENDING.name) },
            )
        }

        @DisplayName("존재하지 않는 결제를 조회하면, 404 Not Found를 반환한다.")
        @Test
        fun returnsNotFound_whenPaymentDoesNotExist() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/999",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("POST /api/v1/payments/recover")
    @Nested
    inner class Recover {

        @DisplayName("PENDING 결제를 PG에서 조회해 SUCCESS로 복구한다.")
        @Test
        fun recoversPendingPaymentToSuccess() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )
            whenever(pgClient.requestPayment(any(), any())).thenReturn(
                PgPaymentResponse(transactionKey = "20250320:TR:abc123", status = "PENDING"),
            )
            val payRequest = PaymentV1Dto.PayRequest(orderId = 1L, cardType = "SAMSUNG", cardNo = "1234-5678-9012-3456", amount = 10000L)
            testRestTemplate.exchange(
                PAYMENT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(payRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {},
            )

            val payment = paymentJpaRepository.findAll().first()

            // 복구 임계값(10분) 통과를 위해 created_at을 11분 전으로 조작 (SQL 내부 연산으로 타임존 변환 없이 처리)
            jdbcTemplate.update(
                "UPDATE payments SET created_at = DATE_SUB(created_at, INTERVAL 11 MINUTE) WHERE id = ?",
                payment.id,
            )

            whenever(pgClient.getPaymentByOrderId(any(), any())).thenReturn(
                PgOrderPaymentResponse(
                    orderId = payment.pgOrderId,
                    transactions = listOf(
                        PgTransactionItem(transactionKey = "20250320:TR:abc123", status = "SUCCESS"),
                    ),
                ),
            )

            // act — 수동 복구 호출
            val response = testRestTemplate.exchange(
                "$PAYMENT_ENDPOINT/recover",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            // assert
            val updatedPayment = paymentJpaRepository.findById(payment.id).get()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS) },
                { assertThat(updatedPayment.pgTransactionKey).isEqualTo("20250320:TR:abc123") },
            )
        }
    }
}
