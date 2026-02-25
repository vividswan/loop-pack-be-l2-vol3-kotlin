package com.loopers.interfaces.api

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.member.MemberModel
import com.loopers.domain.product.ProductModel
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.brand.BrandJpaRepository
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
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

    private fun createProduct(name: String, price: Long, stock: Int): ProductModel {
        val brand = brandJpaRepository.findAll().firstOrNull()
            ?: brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
        return productJpaRepository.save(
            ProductModel.create(name = name, price = price, stock = stock, brandId = brand.id),
        )
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    inner class CreateOrder {

        @DisplayName("유효한 주문 요청하면, 201 Created 응답을 받는다.")
        @Test
        fun returnsCreated_whenValidRequest() {
            // arrange
            createMember()
            val product = createProduct("운동화", 50000L, 10)
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
                { assertThat(response.body?.data?.status).isEqualTo("CREATED") },
                { assertThat(response.body?.data?.totalPrice).isEqualTo(100000L) },
                { assertThat(response.body?.data?.orderItems).hasSize(1) },
            )
        }

        @DisplayName("주문 후 상품의 재고가 차감된다.")
        @Test
        fun decreasesProductStock_afterOrder() {
            // arrange
            createMember()
            val product = createProduct("운동화", 50000L, 10)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 3),
                ),
            )

            // act
            testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
            )

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertThat(updatedProduct.stock).isEqualTo(7)
        }

        @DisplayName("재고가 부족하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenStockIsNotEnough() {
            // arrange
            createMember()
            val product = createProduct("운동화", 50000L, 3)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 5),
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
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("여러 상품을 주문하면, 각 상품의 재고가 차감된다.")
        @Test
        fun decreasesStockForEachProduct_whenMultipleProducts() {
            // arrange
            createMember()
            val product1 = createProduct("운동화", 50000L, 10)
            val product2 = createProduct("티셔츠", 30000L, 5)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product1.id, quantity = 2),
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product2.id, quantity = 3),
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
                { assertThat(response.body?.data?.totalPrice).isEqualTo(190000L) },
                { assertThat(productJpaRepository.findById(product1.id).get().stock).isEqualTo(8) },
                { assertThat(productJpaRepository.findById(product2.id).get().stock).isEqualTo(2) },
            )
        }

        @DisplayName("인증 없이 주문 요청하면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val product = createProduct("운동화", 50000L, 10)
            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 1),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api/v1/orders/{id}")
    @Nested
    inner class GetOrder {

        @DisplayName("주문 상세 조회하면, 200 OK와 주문 정보를 반환한다.")
        @Test
        fun returnsOkWithOrderDetail() {
            // arrange
            createMember()
            val product = createProduct("운동화", 50000L, 10)
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            val createRequest = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.CreateRequest.OrderItemRequest(productId = product.id, quantity = 2),
                ),
            )

            val createResponse = testRestTemplate.exchange(
                ORDER_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(createRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
            )
            val orderId = createResponse.body?.data?.id

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/$orderId",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(orderId) },
                { assertThat(response.body?.data?.totalPrice).isEqualTo(100000L) },
                { assertThat(response.body?.data?.orderItems).hasSize(1) },
                { assertThat(response.body?.data?.orderItems?.first()?.productName).isEqualTo("운동화") },
            )
        }

        @DisplayName("존재하지 않는 주문을 조회하면, 404 Not Found 응답을 받는다.")
        @Test
        fun returnsNotFound_whenOrderDoesNotExist() {
            // arrange
            createMember()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/999",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
