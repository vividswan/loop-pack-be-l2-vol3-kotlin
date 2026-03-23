package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class PaymentModelTest {

    private fun createPayment(orderId: Long = 1L, amount: Long = 10000L): PaymentModel {
        return PaymentModel.create(
            orderId = orderId,
            memberId = 100L,
            cardType = "SAMSUNG",
            cardNo = "1234-5678-9012-3456",
            amount = amount,
        )
    }

    @DisplayName("결제를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("pgOrderId가 'ORDER-' + 10자리 숫자 형식으로 생성된다.")
        @Test
        fun createsPgOrderId_withCorrectFormat() {
            val payment = createPayment(orderId = 1L)

            assertThat(payment.pgOrderId).isEqualTo("ORDER-0000000001")
        }

        @DisplayName("초기 상태는 PENDING이다.")
        @Test
        fun initialStatus_isPending() {
            val payment = createPayment()

            assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
        }

        @DisplayName("orderId가 크더라도 pgOrderId가 올바르게 생성된다.")
        @Test
        fun createsPgOrderId_forLargeOrderId() {
            val payment = createPayment(orderId = 1234567890L)

            assertThat(payment.pgOrderId).isEqualTo("ORDER-1234567890")
        }
    }

    @DisplayName("결제를 확정할 때,")
    @Nested
    inner class Confirm {

        @DisplayName("PENDING 상태에서 SUCCESS로 전환할 수 있다.")
        @Test
        fun confirmsToSuccess_whenPending() {
            val payment = createPayment()

            payment.confirm(
                pgTransactionKey = "20250320:TR:abc123",
                status = PaymentStatus.SUCCESS,
                failureReason = null,
            )

            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS) },
                { assertThat(payment.pgTransactionKey).isEqualTo("20250320:TR:abc123") },
                { assertThat(payment.failureReason).isNull() },
            )
        }

        @DisplayName("PENDING 상태에서 FAILED로 전환할 수 있다.")
        @Test
        fun confirmsToFailed_whenPending() {
            val payment = createPayment()

            payment.confirm(
                pgTransactionKey = null,
                status = PaymentStatus.FAILED,
                failureReason = "한도초과입니다.",
            )

            assertAll(
                { assertThat(payment.status).isEqualTo(PaymentStatus.FAILED) },
                { assertThat(payment.failureReason).isEqualTo("한도초과입니다.") },
            )
        }

        @DisplayName("이미 SUCCESS 상태이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAlreadySuccess() {
            val payment = createPayment()
            payment.confirm(pgTransactionKey = "key", status = PaymentStatus.SUCCESS, failureReason = null)

            val exception = assertThrows<CoreException> {
                payment.confirm(pgTransactionKey = "key2", status = PaymentStatus.FAILED, failureReason = "오류")
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이미 FAILED 상태이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAlreadyFailed() {
            val payment = createPayment()
            payment.confirm(pgTransactionKey = null, status = PaymentStatus.FAILED, failureReason = "한도초과")

            val exception = assertThrows<CoreException> {
                payment.confirm(pgTransactionKey = "key", status = PaymentStatus.SUCCESS, failureReason = null)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("SUCCESS 상태로 전환 시 transactionKey가 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenSuccessWithNullTransactionKey() {
            val payment = createPayment()

            val exception = assertThrows<CoreException> {
                payment.confirm(pgTransactionKey = null, status = PaymentStatus.SUCCESS, failureReason = null)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
