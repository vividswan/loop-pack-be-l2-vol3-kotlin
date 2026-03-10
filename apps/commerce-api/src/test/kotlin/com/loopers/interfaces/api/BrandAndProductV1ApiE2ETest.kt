package com.loopers.interfaces.api

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.ProductModel
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.brand.BrandV1Dto
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandAndProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private const val BRAND_ENDPOINT = "/api/v1/brands"
        private const val PRODUCT_ENDPOINT = "/api/v1/products"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @DisplayName("POST /api/v1/brands")
    @Nested
    inner class RegisterBrand {

        @DisplayName("유효한 정보로 브랜드 등록 요청하면, 201 Created 응답을 받는다.")
        @Test
        fun returnsCreated_whenValidRequestIsProvided() {
            // arrange
            val request = BrandV1Dto.RegisterRequest(name = "나이키", description = "스포츠 브랜드")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
                { assertThat(response.body?.data?.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("이름이 빈 값이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val request = BrandV1Dto.RegisterRequest(name = "", description = "설명")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/brands")
    @Nested
    inner class GetBrands {

        @DisplayName("브랜드 목록 조회 요청하면, 200 OK와 목록을 반환한다.")
        @Test
        fun returnsOkWithBrandList() {
            // arrange
            brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            brandJpaRepository.save(BrandModel.create(name = "아디다스", description = "스포츠"))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<BrandV1Dto.BrandResponse>>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(2) },
            )
        }
    }

    @DisplayName("POST /api/v1/products")
    @Nested
    inner class RegisterProduct {

        @DisplayName("유효한 정보로 상품 등록 요청하면, 201 Created 응답을 받는다.")
        @Test
        fun returnsCreated_whenValidRequestIsProvided() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            val request = ProductV1Dto.RegisterRequest(
                name = "운동화",
                price = 50000L,
                stock = 100,
                brandId = brand.id,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.name).isEqualTo("운동화") },
                { assertThat(response.body?.data?.price).isEqualTo(50000L) },
                { assertThat(response.body?.data?.stock).isEqualTo(100) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 등록 요청하면, 404 Not Found 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandDoesNotExist() {
            // arrange
            val request = ProductV1Dto.RegisterRequest(
                name = "운동화",
                price = 50000L,
                stock = 100,
                brandId = 999L,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class GetProducts {

        @DisplayName("상품 목록 조회 요청하면, 200 OK와 페이지네이션 응답을 반환한다.")
        @Test
        fun returnsOkWithProductPage() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            productJpaRepository.save(ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id))
            productJpaRepository.save(ProductModel.create(name = "티셔츠", price = 30000L, stock = 20, brandId = brand.id))

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?sort=latest",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.products).hasSize(2) },
                { assertThat(response.body?.data?.totalCount).isEqualTo(2L) },
            )
        }

        @DisplayName("가격 오름차순으로 조회하면, 가격이 낮은 순으로 반환된다.")
        @Test
        fun returnsProductsSortedByPriceAsc() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            productJpaRepository.save(ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id))
            productJpaRepository.save(ProductModel.create(name = "티셔츠", price = 30000L, stock = 20, brandId = brand.id))

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?sort=price_asc",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.products?.first()?.price).isEqualTo(30000L) },
                { assertThat(response.body?.data?.products?.last()?.price).isEqualTo(50000L) },
            )
        }

        @DisplayName("브랜드 ID로 필터링하면, 해당 브랜드의 상품만 반환된다.")
        @Test
        fun returnsOnlyFilteredBrandProducts() {
            // arrange
            val nike = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            val adidas = brandJpaRepository.save(BrandModel.create(name = "아디다스", description = "스포츠"))
            productJpaRepository.save(ProductModel.create(name = "에어맥스", price = 169000L, stock = 10, brandId = nike.id))
            productJpaRepository.save(ProductModel.create(name = "조던", price = 219000L, stock = 5, brandId = nike.id))
            productJpaRepository.save(ProductModel.create(name = "울트라부스트", price = 199000L, stock = 15, brandId = adidas.id))

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?brandId=${nike.id}&sort=latest",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.products).hasSize(2) },
                { assertThat(response.body?.data?.totalCount).isEqualTo(2L) },
                {
                    assertThat(response.body?.data?.products).allMatch { it.brandId == nike.id }
                },
            )
        }

        @DisplayName("페이지네이션이 정상적으로 동작한다.")
        @Test
        fun returnsPaginatedProducts() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            repeat(5) { i ->
                productJpaRepository.save(
                    ProductModel.create(
                        name = "상품-$i",
                        price = (10000 + i * 1000).toLong(),
                        stock = 10,
                        brandId = brand.id,
                    ),
                )
            }

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?sort=price_asc&page=0&size=2",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.products).hasSize(2) },
                { assertThat(response.body?.data?.totalCount).isEqualTo(5L) },
                { assertThat(response.body?.data?.page).isEqualTo(0) },
                { assertThat(response.body?.data?.size).isEqualTo(2) },
            )
        }
    }

    @DisplayName("GET /api/v1/products/{id}")
    @Nested
    inner class GetProductDetail {

        @DisplayName("존재하는 상품을 조회하면, 200 OK와 브랜드 정보를 포함한 상세를 반환한다.")
        @Test
        fun returnsOkWithBrandInfo() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
            val product = productJpaRepository.save(
                ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("운동화") },
                { assertThat(response.body?.data?.brandName).isEqualTo("나이키") },
                { assertThat(response.body?.data?.likeCount).isEqualTo(0) },
            )
        }

        @DisplayName("존재하지 않는 상품을 조회하면, 404 Not Found 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductDoesNotExist() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/999",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
