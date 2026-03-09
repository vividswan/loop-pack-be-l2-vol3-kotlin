package com.loopers.concurrency

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
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.order.OrderV1Dto
import com.loopers.utils.ConcurrencyTestHelper
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
class CouponConcurrencyTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
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

    private fun createProduct(stock: Int = 100): ProductModel {
        val brand = brandJpaRepository.findAll().firstOrNull()
            ?: brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
        return productJpaRepository.save(
            ProductModel.create(name = "운동화", price = 50000L, stock = stock, brandId = brand.id),
        )
    }

    private fun createCoupon(): CouponModel {
        return couponJpaRepository.save(
            CouponModel.create(
                name = "5000원 할인",
                type = CouponType.FIXED,
                value = 5000L,
                minOrderAmount = 10000L,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
    }

    @DisplayName("동일한 발급 쿠폰으로 여러 스레드가 동시에 주문하면, 1건만 성공한다.")
    @Test
    fun onlyOneOrderSucceeds_whenSameCouponUsedConcurrently() {
        // arrange
        val threadCount = 5
        val member = createMember("couponuser")
        val product = createProduct(stock = 100)
        val coupon = createCoupon()
        val issuedCoupon = issuedCouponJpaRepository.save(
            IssuedCouponModel.create(memberId = member.id, couponId = coupon.id),
        )

        // act
        val results = ConcurrencyTestHelper.executeParallel(threadCount) {
            val headers = MemberTestFixture.createAuthHeaders(member.loginId, MemberTestFixture.DEFAULT_PASSWORD)
            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
                couponId = issuedCoupon.id,
            )
            testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, headers),
                object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
            ).statusCode
        }

        // assert
        val successCount = results.count { it == HttpStatus.CREATED }
        val failCount = results.count { it != HttpStatus.CREATED }
        assertThat(successCount).isEqualTo(1)
        assertThat(failCount).isEqualTo(threadCount - 1)

        val updatedIssuedCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
        assertThat(updatedIssuedCoupon.status).isEqualTo(CouponStatus.USED)
    }
}
