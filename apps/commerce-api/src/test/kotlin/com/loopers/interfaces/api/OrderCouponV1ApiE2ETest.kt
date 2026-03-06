package com.loopers.interfaces.api

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCouponModel
import com.loopers.domain.member.MemberModel
import com.loopers.domain.product.ProductModel
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.order.OrderV1Dto
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
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderCouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ORDER_ENDPOINT = "/api/v1/orders"
    }

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

    private fun createProduct(name: String = "운동화", price: Long = 50000L, stock: Int = 10): ProductModel {
        val brand = brandJpaRepository.findAll().firstOrNull()
            ?: brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
        return productJpaRepository.save(
            ProductModel.create(name = name, price = price, stock = stock, brandId = brand.id),
        )
    }

    private fun createFixedCoupon(value: Long = 5000L, minOrderAmount: Long = 10000L): CouponModel {
        return couponJpaRepository.save(
            CouponModel.create(
                name = "${value}원 할인",
                type = CouponType.FIXED,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    private fun createRateCoupon(value: Long = 10L, minOrderAmount: Long = 10000L): CouponModel {
        return couponJpaRepository.save(
            CouponModel.create(
                name = "$value% 할인",
                type = CouponType.RATE,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    private fun issueToMember(memberId: Long, couponId: Long): IssuedCouponModel {
        return issuedCouponJpaRepository.save(
            IssuedCouponModel.create(memberId = memberId, couponId = couponId),
        )
    }

    @DisplayName("주문 + 쿠폰 연동")
    @Nested
    inner class OrderWithCoupon {

        @DisplayName("정액 쿠폰 적용 시, 할인 금액이 정확히 계산된다.")
        @Test
        fun appliesFixedDiscount_correctly() {
            // arrange
            val member = createMember()
            val product = createProduct(price = 50000L)
            val coupon = createFixedCoupon(value = 5000L)
            val issuedCoupon = issueToMember(member.id, coupon.id)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 2),
                ),
                couponId = issuedCoupon.id,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.originalPrice).isEqualTo(100000L) },
                { assertThat(response.body?.data?.discountAmount).isEqualTo(5000L) },
                { assertThat(response.body?.data?.totalPrice).isEqualTo(95000L) },
                { assertThat(response.body?.data?.couponId).isEqualTo(coupon.id) },
            )

            // 쿠폰 상태 USED 확인
            val usedCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertThat(usedCoupon.status).isEqualTo(CouponStatus.USED)
        }

        @DisplayName("정률 쿠폰 적용 시, 할인 금액이 정확히 계산된다.")
        @Test
        fun appliesRateDiscount_correctly() {
            // arrange
            val member = createMember()
            val product = createProduct(price = 50000L)
            val coupon = createRateCoupon(value = 10L)
            val issuedCoupon = issueToMember(member.id, coupon.id)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 2),
                ),
                couponId = issuedCoupon.id,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert - 100000 * 10 / 100 = 10000 할인
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.originalPrice).isEqualTo(100000L) },
                { assertThat(response.body?.data?.discountAmount).isEqualTo(10000L) },
                { assertThat(response.body?.data?.totalPrice).isEqualTo(90000L) },
            )
        }

        @DisplayName("쿠폰 없이 주문하면, 할인 없이 정상 주문된다.")
        @Test
        fun createsOrder_withoutCoupon() {
            // arrange
            val member = createMember()
            val product = createProduct(price = 50000L)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 2),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.originalPrice).isEqualTo(100000L) },
                { assertThat(response.body?.data?.discountAmount).isEqualTo(0L) },
                { assertThat(response.body?.data?.totalPrice).isEqualTo(100000L) },
                { assertThat(response.body?.data?.couponId).isNull() },
            )
        }

        @DisplayName("재고 부족으로 주문 실패하면, 쿠폰도 사용되지 않는다 (트랜잭션 롤백).")
        @Test
        fun rollbacksCoupon_whenStockInsufficient() {
            // arrange
            val member = createMember()
            val product = createProduct(price = 50000L, stock = 1)
            val coupon = createFixedCoupon(value = 5000L)
            val issuedCoupon = issueToMember(member.id, coupon.id)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 5),
                ),
                couponId = issuedCoupon.id,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

            // 쿠폰이 여전히 AVAILABLE인지 확인
            val checkCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertThat(checkCoupon.status).isEqualTo(CouponStatus.AVAILABLE)
        }

        @DisplayName("이미 사용된 쿠폰으로 주문하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenCouponAlreadyUsed() {
            // arrange
            val member = createMember()
            val product = createProduct(price = 50000L)
            val coupon = createFixedCoupon(value = 5000L)
            val issuedCoupon = issueToMember(member.id, coupon.id)

            // 쿠폰을 먼저 사용
            issuedCoupon.use()
            issuedCouponJpaRepository.save(issuedCoupon)

            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
                couponId = issuedCoupon.id,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 쿠폰으로 주문하면, 404 Not Found 응답을 받는다.")
        @Test
        fun returnsNotFound_whenCouponDoesNotExist() {
            // arrange
            val member = createMember()
            val product = createProduct(price = 50000L)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
                couponId = 999999L,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)

            // 재고가 차감되지 않았는지 확인 (트랜잭션 롤백)
            val checkProduct = productJpaRepository.findById(product.id).get()
            assertThat(checkProduct.stock).isEqualTo(10)
        }

        @DisplayName("만료된 쿠폰으로 주문하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenCouponIsExpired() {
            // arrange
            val member = createMember()
            val product = createProduct(price = 50000L)
            val expiredCoupon = couponJpaRepository.save(
                CouponModel.create(
                    name = "만료 쿠폰",
                    type = CouponType.FIXED,
                    value = 5000L,
                    minOrderAmount = 10000L,
                    expiredAt = ZonedDateTime.now().minusDays(1),
                ),
            )
            val issuedCoupon = issueToMember(member.id, expiredCoupon.id)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
                couponId = issuedCoupon.id,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

            // 쿠폰이 여전히 AVAILABLE인지 확인
            val checkCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertThat(checkCoupon.status).isEqualTo(CouponStatus.AVAILABLE)
        }

        @DisplayName("타 유저 소유 쿠폰으로 주문하면, 403 Forbidden 응답을 받는다.")
        @Test
        fun returnsForbidden_whenCouponBelongsToAnotherUser() {
            // arrange
            val ownerMember = createMember()
            val otherMember = memberJpaRepository.save(
                MemberModel.create(
                    loginId = "otheruser",
                    rawPassword = "Other1234!",
                    passwordEncoder = passwordEncoder,
                    name = "김철수",
                    birthDate = LocalDate.of(1995, 5, 15),
                    email = "other@example.com",
                ),
            )
            val product = createProduct(price = 50000L)
            val coupon = createFixedCoupon(value = 5000L)
            val issuedCoupon = issueToMember(ownerMember.id, coupon.id)

            // 타 유저의 인증 헤더
            val headers = MemberTestFixture.createAuthHeaders("otheruser", "Other1234!")

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
                couponId = issuedCoupon.id,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

            // 쿠폰이 여전히 AVAILABLE인지 확인
            val checkCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
            assertThat(checkCoupon.status).isEqualTo(CouponStatus.AVAILABLE)
        }

        @DisplayName("최소 주문금액 미달 시, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenMinOrderAmountNotMet() {
            // arrange
            val member = createMember()
            val product = createProduct(price = 5000L)
            val coupon = createFixedCoupon(value = 3000L, minOrderAmount = 50000L)
            val issuedCoupon = issueToMember(member.id, coupon.id)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
                couponId = issuedCoupon.id,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
