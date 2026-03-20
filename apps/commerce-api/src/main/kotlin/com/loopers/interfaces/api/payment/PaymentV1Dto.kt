package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentInfo
import com.loopers.domain.payment.PaymentCommand

class PaymentV1Dto {

    data class PayRequest(
        val orderId: Long,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
    ) {
        fun toCommand(memberId: Long): PaymentCommand.RequestPayment {
            return PaymentCommand.RequestPayment(
                orderId = orderId,
                memberId = memberId,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
            )
        }
    }

    data class PaymentResponse(
        val id: Long,
        val orderId: Long,
        val pgOrderId: String,
        val cardType: String,
        val amount: Long,
        val status: String,
        val pgTransactionKey: String?,
        val failureReason: String?,
    ) {
        companion object {
            fun from(info: PaymentInfo): PaymentResponse {
                return PaymentResponse(
                    id = info.id,
                    orderId = info.orderId,
                    pgOrderId = info.pgOrderId,
                    cardType = info.cardType,
                    amount = info.amount,
                    status = info.status,
                    pgTransactionKey = info.pgTransactionKey,
                    failureReason = info.failureReason,
                )
            }
        }
    }
}
