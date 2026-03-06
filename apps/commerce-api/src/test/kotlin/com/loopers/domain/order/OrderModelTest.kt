package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderModelTest {

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("유효한 주문 항목이 있으면, 주문이 정상적으로 생성된다.")
        @Test
        fun createsOrder_whenItemsAreValid() {
            // arrange
            val memberId = 1L
            val items = listOf(
                OrderItemModel.create(productId = 1L, productName = "운동화", quantity = 2, price = 50000L),
                OrderItemModel.create(productId = 2L, productName = "티셔츠", quantity = 1, price = 30000L),
            )

            // act
            val order = OrderModel.create(memberId = memberId, items = items)

            // assert
            assertAll(
                { assertThat(order.memberId).isEqualTo(memberId) },
                { assertThat(order.status).isEqualTo(OrderStatus.CREATED) },
                { assertThat(order.orderItems).hasSize(2) },
                { assertThat(order.totalPrice).isEqualTo(130000L) },
            )
        }

        @DisplayName("주문 항목이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenItemsAreEmpty() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                OrderModel.create(memberId = 1L, items = emptyList())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("주문 항목을 추가할 때,")
    @Nested
    inner class AddItem {

        @DisplayName("항목을 추가하면, 총 금액이 업데이트된다.")
        @Test
        fun updatesTotalPrice_whenItemIsAdded() {
            // arrange
            val items = listOf(
                OrderItemModel.create(productId = 1L, productName = "운동화", quantity = 1, price = 50000L),
            )
            val order = OrderModel.create(memberId = 1L, items = items)

            // act
            order.addItem(
                OrderItemModel.create(productId = 2L, productName = "티셔츠", quantity = 3, price = 20000L),
            )

            // assert
            assertAll(
                { assertThat(order.orderItems).hasSize(2) },
                { assertThat(order.totalPrice).isEqualTo(110000L) },
            )
        }
    }

    @DisplayName("주문을 취소할 때,")
    @Nested
    inner class Cancel {

        @DisplayName("주문 상태가 CANCELLED로 변경된다.")
        @Test
        fun changesStatusToCancelled() {
            // arrange
            val items = listOf(
                OrderItemModel.create(productId = 1L, productName = "운동화", quantity = 1, price = 50000L),
            )
            val order = OrderModel.create(memberId = 1L, items = items)

            // act
            order.cancel()

            // assert
            assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
        }
    }

    @DisplayName("주문 항목의 총 금액 계산 시,")
    @Nested
    inner class OrderItemTotalPrice {

        @DisplayName("가격 * 수량으로 계산된다.")
        @Test
        fun calculatesTotalPrice() {
            // arrange
            val item = OrderItemModel.create(productId = 1L, productName = "운동화", quantity = 3, price = 50000L)

            // act
            val totalPrice = item.getTotalPrice()

            // assert
            assertThat(totalPrice).isEqualTo(150000L)
        }

        @DisplayName("수량이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsNotPositive() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                OrderItemModel.create(productId = 1L, productName = "운동화", quantity = 0, price = 50000L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("가격이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPriceIsNegative() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                OrderItemModel.create(productId = 1L, productName = "운동화", quantity = 1, price = -1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
