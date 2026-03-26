package com.loopers.concurrency

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.member.MemberModel
import com.loopers.domain.product.ProductModel
import com.loopers.fixtures.MemberTestFixture
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.like.LikeV1Dto
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeConcurrencyTest @Autowired constructor(
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

    private fun createProduct(): ProductModel {
        val brand = brandJpaRepository.findAll().firstOrNull()
            ?: brandJpaRepository.save(BrandModel.create(name = "나이키", description = "스포츠"))
        return productJpaRepository.save(
            ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = brand.id),
        )
    }

    @DisplayName("10명이 동시에 좋아요하면, likeCount가 정확히 10이 된다.")
    @Test
    fun likeCountIsExact_whenConcurrentLikes() {
        // arrange
        val threadCount = 10
        val product = createProduct()
        val members = (1..threadCount).map { createMember("likeuser$it") }

        // act
        val results = ConcurrencyTestHelper.executeParallel(threadCount) { index ->
            val member = members[index]
            val headers = MemberTestFixture.createAuthHeaders(member.loginId, MemberTestFixture.DEFAULT_PASSWORD)
            testRestTemplate.exchange(
                "/api/v1/products/${product.id}/likes",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {},
            ).statusCode
        }

        // assert
        val successCount = results.count { it == HttpStatus.CREATED }
        val failCount = results.count { it != HttpStatus.CREATED }
        assertThat(successCount).isEqualTo(threadCount)
        assertThat(failCount).isEqualTo(0)

        // 비동기 집계 처리 대기 (AFTER_COMMIT + @Async + REQUIRES_NEW)
        Thread.sleep(3000)

        val updatedProduct = productJpaRepository.findById(product.id).get()
        assertThat(updatedProduct.likeCount).isEqualTo(threadCount)
    }
}
