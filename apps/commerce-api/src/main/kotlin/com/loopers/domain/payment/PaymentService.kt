package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class PaymentService(
    private val paymentRepository: PaymentRepository,
) {
    @Transactional
    fun createPendingPayment(command: PaymentCommand.RequestPayment): PaymentModel {
        val payment = PaymentModel.create(
            orderId = command.orderId,
            memberId = command.memberId,
            cardType = command.cardType,
            cardNo = command.cardNo,
            amount = command.amount,
        )
        return paymentRepository.save(payment)
    }

    @Transactional
    fun confirmPayment(command: PaymentCommand.ConfirmPayment): PaymentModel {
        val payment = paymentRepository.findByPgOrderId(command.pgOrderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, PaymentErrorCode.NOT_FOUND)
        payment.confirm(command.pgTransactionKey, command.status, command.failureReason)
        return paymentRepository.save(payment)
    }

    fun getPayment(id: Long): PaymentModel {
        return paymentRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, PaymentErrorCode.NOT_FOUND)
    }

    fun findPendingPaymentsOlderThan(minutes: Long): List<PaymentModel> {
        val threshold = ZonedDateTime.now().minusMinutes(minutes)
        return paymentRepository.findPendingOlderThan(threshold)
    }
}
