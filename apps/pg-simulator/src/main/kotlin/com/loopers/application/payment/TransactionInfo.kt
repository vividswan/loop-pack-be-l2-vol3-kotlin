package com.loopers.application.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.TransactionStatus

data class TransactionInfo(
    val transactionKey: String,
    val orderId: String,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
    val status: TransactionStatus,
    val reason: String?,
) {
    companion object {
        fun from(payment: Payment): TransactionInfo =
            TransactionInfo(
                transactionKey = payment.transactionKey,
                orderId = payment.orderId,
                cardType = payment.cardType,
                cardNo = payment.cardNo,
                amount = payment.amount,
                status = payment.status,
                reason = payment.reason,
            )
    }
}
