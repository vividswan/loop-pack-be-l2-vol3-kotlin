package com.loopers.domain.payment

class PaymentCommand {
    data class RequestPayment(
        val orderId: Long,
        val memberId: Long,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
    )

    data class ConfirmPayment(
        val pgOrderId: String,
        val pgTransactionKey: String?,
        val status: PaymentStatus,
        val failureReason: String?,
    )
}
