package com.loopers.domain.order

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var orderService: OrderService

    @DisplayName("주문 생성 시,")
    @Nested
    inner class CreateOrder {

        @DisplayName("유효한 주문이면, 주문이 저장되고 재고가 차감된다.")
        @Test
        fun savesOrderAndDecreasesStock_whenOrderIsValid() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)
            whenever(productRepository.findById(1L)).thenReturn(product)
            whenever(orderRepository.save(any())).thenAnswer { it.getArgument<OrderModel>(0) }

            val commands = listOf(
                OrderService.OrderItemCommand(productId = 1L, quantity = 2),
            )

            // act
            val result = orderService.createOrder(memberId = 1L, orderItemCommands = commands)

            // assert
            assertAll(
                { assertThat(result.memberId).isEqualTo(1L) },
                { assertThat(result.status).isEqualTo(OrderStatus.CREATED) },
                { assertThat(result.orderItems).hasSize(1) },
                { assertThat(result.totalPrice).isEqualTo(100000L) },
                { assertThat(product.stock).isEqualTo(8) },
            )
        }

        @DisplayName("여러 상품을 주문하면, 각 상품의 재고가 차감된다.")
        @Test
        fun decreasesStockForEachProduct_whenMultipleProducts() {
            // arrange
            val product1 = ProductModel.create(name = "운동화", price = 50000L, stock = 10, brandId = 1L)
            val product2 = ProductModel.create(name = "티셔츠", price = 30000L, stock = 5, brandId = 1L)
            whenever(productRepository.findById(1L)).thenReturn(product1)
            whenever(productRepository.findById(2L)).thenReturn(product2)
            whenever(orderRepository.save(any())).thenAnswer { it.getArgument<OrderModel>(0) }

            val commands = listOf(
                OrderService.OrderItemCommand(productId = 1L, quantity = 2),
                OrderService.OrderItemCommand(productId = 2L, quantity = 3),
            )

            // act
            val result = orderService.createOrder(memberId = 1L, orderItemCommands = commands)

            // assert
            assertAll(
                { assertThat(result.totalPrice).isEqualTo(190000L) },
                { assertThat(product1.stock).isEqualTo(8) },
                { assertThat(product2.stock).isEqualTo(2) },
            )
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenStockIsNotEnough() {
            // arrange
            val product = ProductModel.create(name = "운동화", price = 50000L, stock = 3, brandId = 1L)
            whenever(productRepository.findById(1L)).thenReturn(product)

            val commands = listOf(
                OrderService.OrderItemCommand(productId = 1L, quantity = 5),
            )

            // act
            val exception = assertThrows<CoreException> {
                orderService.createOrder(memberId = 1L, orderItemCommands = commands)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductDoesNotExist() {
            // arrange
            whenever(productRepository.findById(999L)).thenReturn(null)

            val commands = listOf(
                OrderService.OrderItemCommand(productId = 999L, quantity = 1),
            )

            // act
            val exception = assertThrows<CoreException> {
                orderService.createOrder(memberId = 1L, orderItemCommands = commands)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("주문 조회 시,")
    @Nested
    inner class GetOrder {

        @DisplayName("존재하는 ID로 조회하면, 주문이 반환된다.")
        @Test
        fun returnsOrder_whenIdExists() {
            // arrange
            val items = listOf(
                OrderItemModel.create(productId = 1L, productName = "운동화", quantity = 1, price = 50000L),
            )
            val order = OrderModel.create(memberId = 1L, items = items)
            whenever(orderRepository.findById(1L)).thenReturn(order)

            // act
            val result = orderService.getOrder(1L)

            // assert
            assertThat(result.memberId).isEqualTo(1L)
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenIdDoesNotExist() {
            // arrange
            whenever(orderRepository.findById(999L)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
