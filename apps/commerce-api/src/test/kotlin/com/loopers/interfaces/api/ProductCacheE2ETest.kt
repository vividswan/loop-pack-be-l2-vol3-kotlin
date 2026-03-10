package com.loopers.interfaces.api

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.member.MemberModel
import com.loopers.domain.product.ProductModel
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.product.ProductLocalCacheManager
import com.loopers.interfaces.api.like.LikeV1Dto
import com.loopers.interfaces.api.product.ProductV1Dto
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductCacheE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val defaultRedisTemplate: RedisTemplate<String, String>,
    private val productLocalCacheManager: ProductLocalCacheManager,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private const val PRODUCT_ENDPOINT = "/api/v1/products"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
        productLocalCacheManager.evictAll()
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

    @DisplayName("상품 상세 캐시")
    @Nested
    inner class ProductDetailCache {

        @DisplayName("상품 상세 조회 시 Redis에 캐시가 저장된다.")
        @Test
        fun cachesProductDetailInRedis() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            val product = productJpaRepository.save(
                ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id),
            )

            // act
            testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {},
            )

            // assert
            val cachedValue = defaultRedisTemplate.opsForValue().get("product:detail:${product.id}")
            assertThat(cachedValue).isNotNull()
        }

        @DisplayName("캐시된 상품을 다시 조회하면 동일한 결과를 반환한다.")
        @Test
        fun returnsCachedProductDetail() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            val product = productJpaRepository.save(
                ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id),
            )

            // act - 첫 번째 조회 (캐시 생성)
            val firstResponse = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {},
            )

            // act - 두 번째 조회 (캐시 히트)
            val secondResponse = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(firstResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(secondResponse.body?.data?.name).isEqualTo(firstResponse.body?.data?.name) },
                { assertThat(secondResponse.body?.data?.price).isEqualTo(firstResponse.body?.data?.price) },
            )
        }

        @DisplayName("좋아요 후 상품 상세 Redis 캐시가 무효화된다.")
        @Test
        fun evictsRedisCacheAfterLike() {
            // arrange
            createMember()
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            val product = productJpaRepository.save(
                ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id),
            )
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // 캐시 생성
            testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {},
            )
            assertThat(defaultRedisTemplate.opsForValue().get("product:detail:${product.id}")).isNotNull()

            // act - 좋아요 등록 (AFTER_COMMIT 시점에 Redis + 로컬 캐시 무효화)
            testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {},
            )

            // assert - Redis 캐시가 무효화됨
            val cachedValue = defaultRedisTemplate.opsForValue().get("product:detail:${product.id}")
            assertThat(cachedValue).isNull()
        }

        @DisplayName("좋아요 후 다시 조회하면 최신 좋아요 수가 반영된다.")
        @Test
        fun returnsUpdatedLikeCountAfterCacheEviction() {
            // arrange
            createMember()
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            val product = productJpaRepository.save(
                ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id),
            )
            val headers = MemberTestFixture.createAuthHeaders(
                MemberTestFixture.DEFAULT_LOGIN_ID,
                MemberTestFixture.DEFAULT_PASSWORD,
            )

            // 캐시 생성 (likeCount = 0)
            testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {},
            )

            // 좋아요 등록 (AFTER_COMMIT 시점에 Redis + 로컬 캐시 무효화)
            testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {},
            )

            // act - 다시 조회 (캐시 재생성, likeCount = 1)
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {},
            )

            // assert
            assertThat(response.body?.data?.likeCount).isEqualTo(1)
        }
    }

    @DisplayName("상품 목록 캐시")
    @Nested
    inner class ProductListCache {

        @DisplayName("상품 목록 조회 시 Redis에 캐시가 저장된다.")
        @Test
        fun cachesProductListInRedis() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            productJpaRepository.save(ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id))

            // act
            testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?brandId=${brand.id}&sort=latest&page=0&size=20",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {},
            )

            // assert
            val cachedValue = defaultRedisTemplate.opsForValue().get("product:list:${brand.id}:LATEST:0:20")
            assertThat(cachedValue).isNotNull()
        }

        @DisplayName("캐시된 목록을 다시 조회하면 동일한 결과를 반환한다.")
        @Test
        fun returnsCachedProductList() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            productJpaRepository.save(ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id))
            productJpaRepository.save(ProductModel.create(name = "티셔츠", price = 30000L, stock = 20, brandId = brand.id))

            // act - 첫 번째 조회 (캐시 생성)
            val firstResponse = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?sort=latest&page=0&size=20",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {},
            )

            // act - 두 번째 조회 (캐시 히트)
            val secondResponse = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?sort=latest&page=0&size=20",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(firstResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(secondResponse.body?.data?.totalCount).isEqualTo(firstResponse.body?.data?.totalCount) },
                {
                    assertThat(secondResponse.body?.data?.products?.size)
                        .isEqualTo(firstResponse.body?.data?.products?.size)
                },
            )
        }
    }
}
