package com.loopers.infrastructure.payment.pg

data class PgPaymentRequest(
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: String,
    val callbackUrl: String,
)
