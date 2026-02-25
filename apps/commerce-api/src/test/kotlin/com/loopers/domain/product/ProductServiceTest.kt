package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var productService: ProductService

    @DisplayName("상품 등록 시,")
    @Nested
    inner class Register {

        @DisplayName("유효한 정보로 등록하면, 상품이 저장된다.")
        @Test
        fun savesProduct_whenValidInfoIsProvided() {
            // arrange
            whenever(productRepository.save(any())).thenAnswer { it.getArgument<ProductModel>(0) }

            // act
            val result = productService.register("운동화", 50000L, 100, 1L)

            // assert
            assertThat(result.name).isEqualTo("운동화")
            assertThat(result.price).isEqualTo(50000L)
            assertThat(result.stock).isEqualTo(100)
        }
    }

    @DisplayName("상품 조회 시,")
    @Nested
    inner class GetProduct {

        @DisplayName("존재하는 ID로 조회하면, 상품이 반환된다.")
        @Test
        fun returnsProduct_whenIdExists() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 100, brandId = 1L)
            whenever(productRepository.findById(1L)).thenReturn(product)

            // act
            val result = productService.getProduct(1L)

            // assert
            assertThat(result.name).isEqualTo("운동화")
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenIdDoesNotExist() {
            // arrange
            whenever(productRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                productService.getProduct(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 목록 조회 시,")
    @Nested
    inner class GetProducts {

        @DisplayName("최신순으로 조회하면, findAllOrderByCreatedAtDesc가 호출된다.")
        @Test
        fun callsFindAllOrderByCreatedAtDesc_whenSortTypeIsLatest() {
            // arrange
            val products = listOf(
                ProductModel.create(name = "상품1", price = 10000L, stock = 10, brandId = 1L),
            )
            whenever(productRepository.findAllOrderByCreatedAtDesc()).thenReturn(products)

            // act
            val result = productService.getProducts(ProductSortType.LATEST)

            // assert
            assertThat(result).hasSize(1)
        }

        @DisplayName("가격 오름차순으로 조회하면, findAllOrderByPriceAsc가 호출된다.")
        @Test
        fun callsFindAllOrderByPriceAsc_whenSortTypeIsPriceAsc() {
            // arrange
            val products = listOf(
                ProductModel.create(name = "상품1", price = 10000L, stock = 10, brandId = 1L),
            )
            whenever(productRepository.findAllOrderByPriceAsc()).thenReturn(products)

            // act
            val result = productService.getProducts(ProductSortType.PRICE_ASC)

            // assert
            assertThat(result).hasSize(1)
        }

        @DisplayName("좋아요 내림차순으로 조회하면, findAllOrderByLikeCountDesc가 호출된다.")
        @Test
        fun callsFindAllOrderByLikeCountDesc_whenSortTypeIsLikesDesc() {
            // arrange
            val products = listOf(
                ProductModel.create(name = "상품1", price = 10000L, stock = 10, brandId = 1L),
            )
            whenever(productRepository.findAllOrderByLikeCountDesc()).thenReturn(products)

            // act
            val result = productService.getProducts(ProductSortType.LIKES_DESC)

            // assert
            assertThat(result).hasSize(1)
        }
    }
}
