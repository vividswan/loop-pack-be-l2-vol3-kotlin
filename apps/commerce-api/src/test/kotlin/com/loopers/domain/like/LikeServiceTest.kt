package com.loopers.domain.like

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class LikeServiceTest {

    @Mock
    private lateinit var likeRepository: LikeRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var likeService: LikeService

    @DisplayName("좋아요 등록 시,")
    @Nested
    inner class Like {

        @DisplayName("처음 좋아요하면, 좋아요가 저장되고 상품의 좋아요 수가 증가한다.")
        @Test
        fun savesLikeAndIncreasesLikeCount_whenFirstLike() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)
            whenever(productRepository.findById(1L)).thenReturn(product)
            whenever(likeRepository.existsByMemberIdAndProductId(1L, 1L)).thenReturn(false)
            whenever(likeRepository.save(any())).thenAnswer { it.getArgument<LikeModel>(0) }

            // act
            val result = likeService.like(memberId = 1L, productId = 1L)

            // assert
            assertThat(result.memberId).isEqualTo(1L)
            assertThat(result.productId).isEqualTo(1L)
            assertThat(product.likeCount).isEqualTo(1)
        }

        @DisplayName("이미 좋아요한 상품이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenAlreadyLiked() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)
            whenever(productRepository.findById(1L)).thenReturn(product)
            whenever(likeRepository.existsByMemberIdAndProductId(1L, 1L)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                likeService.like(memberId = 1L, productId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductDoesNotExist() {
            // arrange
            whenever(productRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                likeService.like(memberId = 1L, productId = 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("좋아요 취소 시,")
    @Nested
    inner class Unlike {

        @DisplayName("좋아요 기록이 있으면, 삭제되고 상품의 좋아요 수가 감소한다.")
        @Test
        fun deletesLikeAndDecreasesLikeCount_whenLikeExists() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)
            product.increaseLikeCount()
            val like = LikeModel.create(memberId = 1L, productId = 1L)

            whenever(productRepository.findById(1L)).thenReturn(product)
            whenever(likeRepository.findByMemberIdAndProductId(1L, 1L)).thenReturn(like)

            // act
            likeService.unlike(memberId = 1L, productId = 1L)

            // assert
            verify(likeRepository).delete(like)
            assertThat(product.likeCount).isEqualTo(0)
        }

        @DisplayName("좋아요 기록이 없으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenLikeDoesNotExist() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)
            whenever(productRepository.findById(1L)).thenReturn(product)
            whenever(likeRepository.findByMemberIdAndProductId(1L, 1L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                likeService.unlike(memberId = 1L, productId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductDoesNotExist() {
            // arrange
            whenever(productRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                likeService.unlike(memberId = 1L, productId = 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
