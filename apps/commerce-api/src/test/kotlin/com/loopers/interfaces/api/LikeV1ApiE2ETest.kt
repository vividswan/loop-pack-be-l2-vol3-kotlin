package com.loopers.interfaces.api

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.member.MemberModel
import com.loopers.domain.product.ProductModel
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.like.LikeV1Dto
import com.loopers.interfaces.api.product.ProductV1Dto
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
class LikeV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
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

    private fun createProduct(): ProductModel {
        val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
        return productJpaRepository.save(
            ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id),
        )
    }

    @DisplayName("POST /api/v1/products/{productId}/likes")
    @Nested
    inner class LikeProduct {

        @DisplayName("유효한 인증으로 좋아요 등록하면, 201 Created 응답을 받는다.")
        @Test
        fun returnsCreated_whenValidRequest() {
            // arrange
            createMember()
            val product = createProduct()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.productId).isEqualTo(product.id) },
            )
        }

        @DisplayName("좋아요 등록 후 상품 조회하면, 좋아요 수가 1 증가되어 있다.")
        @Test
        fun increasesLikeCount_afterLike() {
            // arrange
            createMember()
            val product = createProduct()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act - 좋아요 등록
            testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {},
            )

            // assert - 상품 조회
            val productResponse = testRestTemplate.exchange(
                "/api/v1/products/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {},
            )
            assertThat(productResponse.body?.data?.likeCount).isEqualTo(1)
        }

        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면, 409 Conflict 응답을 받는다.")
        @Test
        fun returnsConflict_whenAlreadyLiked() {
            // arrange
            createMember()
            val product = createProduct()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // 첫 번째 좋아요
            testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {},
            )

            // act - 두 번째 좋아요
            val responseType = object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("인증 없이 좋아요 등록하면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val product = createProduct()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("DELETE /api/v1/products/{productId}/likes")
    @Nested
    inner class UnlikeProduct {

        @DisplayName("좋아요 취소하면, 200 OK 응답을 받고 좋아요 수가 감소한다.")
        @Test
        fun returnsOkAndDecreasesLikeCount() {
            // arrange
            createMember()
            val product = createProduct()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // 좋아요 등록
            testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {},
            )

            // act - 좋아요 취소
            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.DELETE,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            val productResponse = testRestTemplate.exchange(
                "/api/v1/products/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {},
            )
            assertThat(productResponse.body?.data?.likeCount).isEqualTo(0)
        }

        @DisplayName("좋아요하지 않은 상품을 취소하면, 404 Not Found 응답을 받는다.")
        @Test
        fun returnsNotFound_whenNotLiked() {
            // arrange
            createMember()
            val product = createProduct()
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.DELETE,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
