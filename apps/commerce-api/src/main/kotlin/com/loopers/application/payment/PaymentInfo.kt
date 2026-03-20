package com.loopers.application.payment

import com.loopers.domain.payment.PaymentModel

data class PaymentInfo(
    val id: Long,
    val orderId: Long,
    val memberId: Long,
    val pgOrderId: String,
    val cardType: String,
    val amount: Long,
    val status: String,
    val pgTransactionKey: String?,
    val failureReason: String?,
) {
    companion object {
        fun from(model: PaymentModel): PaymentInfo {
            return PaymentInfo(
                id = model.id,
                orderId = model.orderId,
                memberId = model.memberId,
                pgOrderId = model.pgOrderId,
                cardType = model.cardType,
                amount = model.amount,
                status = model.status.name,
                pgTransactionKey = model.pgTransactionKey,
                failureReason = model.failureReason,
            )
        }
    }
}
