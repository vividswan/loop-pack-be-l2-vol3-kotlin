package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ProductModelTest {

    @DisplayName("상품을 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("모든 필드가 유효하면, 상품이 정상적으로 생성된다.")
        @Test
        fun createsProduct_whenAllFieldsAreValid() {
            // arrange
            val name = "운동화"
            val price = 50000L
            val stock = 100
            val brandId = 1L

            // act
            val product = ProductModel.create(name = name, price = price, stock = stock, brandId = brandId)

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo(name) },
                { assertThat(product.price).isEqualTo(price) },
                { assertThat(product.stock).isEqualTo(stock) },
                { assertThat(product.brandId).isEqualTo(brandId) },
                { assertThat(product.likeCount).isEqualTo(0) },
            )
        }

        @DisplayName("이름이 빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        fun throwsBadRequest_whenNameIsBlank(name: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                ProductModel.create(name = name, price = 50000L, stock = 100, brandId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("가격이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPriceIsNegative() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                ProductModel.create(name = "운동화", price = -1L, stock = 100, brandId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("재고가 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenStockIsNegative() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                ProductModel.create(name = "운동화", price = 50000L, stock = -1, brandId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    inner class DecreaseStock {

        @DisplayName("충분한 재고가 있으면, 정상적으로 차감된다.")
        @Test
        fun decreasesStock_whenStockIsSufficient() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)

            // act
            product.decreaseStock(3)

            // assert
            assertThat(product.stock).isEqualTo(7)
        }

        @DisplayName("재고와 동일한 수량을 차감하면, 재고가 0이 된다.")
        @Test
        fun decreasesStockToZero_whenQuantityEqualsStock() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 5, brandId = 1L)

            // act
            product.decreaseStock(5)

            // assert
            assertThat(product.stock).isEqualTo(0)
        }

        @DisplayName("재고보다 많은 수량을 차감하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenStockIsNotEnough() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 3, brandId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                product.decreaseStock(5)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("좋아요 수를 변경할 때,")
    @Nested
    inner class LikeCount {

        @DisplayName("좋아요 수를 증가시키면, 1 증가한다.")
        @Test
        fun increasesLikeCount() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)

            // act
            product.increaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(1)
        }

        @DisplayName("좋아요 수를 감소시키면, 1 감소한다.")
        @Test
        fun decreasesLikeCount() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)
            product.increaseLikeCount()
            product.increaseLikeCount()

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(1)
        }

        @DisplayName("좋아요 수가 0일 때 감소시키면, 0을 유지한다.")
        @Test
        fun keepsZero_whenLikeCountIsAlreadyZero() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)

            // act
            product.decreaseLikeCount()

            // assert
            assertThat(product.likeCount).isEqualTo(0)
        }
    }
}
