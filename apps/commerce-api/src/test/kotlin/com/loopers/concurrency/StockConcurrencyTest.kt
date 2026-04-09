package com.loopers.concurrency

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.member.MemberModel
import com.loopers.domain.product.ProductModel
import com.loopers.domain.queue.QueueRepository
import com.loopers.fixtures.MemberTestFixture
import com.loopers.fixtures.QueueTestFixture
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.order.OrderV1Dto
import com.loopers.utils.ConcurrencyTestHelper
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StockConcurrencyTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val queueRepository: QueueRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
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

    private fun createProduct(stock: Int): ProductModel {
        val brand = brandJpaRepository.findAll().firstOrNull()
            ?: brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
        return productJpaRepository.save(
            ProductModel.create(name = "운동화", price = 50000L, stock = stock, brandId = brand.id),
        )
    }

    @DisplayName("재고 10개 상품에 10명이 동시 주문하면, 모든 주문이 성공하고 재고가 0이 된다.")
    @Test
    fun allOrdersSucceed_whenStockIsSufficient() {
        // arrange
        val threadCount = 10
        val product = createProduct(stock = threadCount)
        val members = (1..threadCount).map { createMember("user$it") }
        val tokens = members.map { QueueTestFixture.issueTestToken(queueRepository, it.id) }

        // act
        val results = ConcurrencyTestHelper.executeParallel(threadCount) { index ->
            val member = members[index]
            val headers = MemberTestFixture.createAuthHeaders(member.loginId, MemberTestFixture.DEFAULT_PASSWORD)
            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
                entryToken = tokens[index],
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
        assertThat(successCount).isEqualTo(threadCount)

        val updatedProduct = productJpaRepository.findById(product.id).get()
        assertThat(updatedProduct.stock).isEqualTo(0)
    }

    @DisplayName("재고 5개 상품에 10명이 동시 주문하면, 5명만 성공하고 나머지는 실패한다.")
    @Test
    fun onlyAvailableStockOrdersSucceed_whenStockIsInsufficient() {
        // arrange
        val threadCount = 10
        val stock = 5
        val product = createProduct(stock = stock)
        val members = (1..threadCount).map { createMember("user$it") }
        val tokens = members.map { QueueTestFixture.issueTestToken(queueRepository, it.id) }

        // act
        val results = ConcurrencyTestHelper.executeParallel(threadCount) { index ->
            val member = members[index]
            val headers = MemberTestFixture.createAuthHeaders(member.loginId, MemberTestFixture.DEFAULT_PASSWORD)
            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
                entryToken = tokens[index],
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
        val failCount = results.count { it == HttpStatus.BAD_REQUEST }
        assertThat(successCount).isEqualTo(stock)
        assertThat(failCount).isEqualTo(threadCount - stock)

        val updatedProduct = productJpaRepository.findById(product.id).get()
        assertThat(updatedProduct.stock).isEqualTo(0)
    }
}
