package com.loopers.application.payment

data class OrderInfo(
    val orderId: String,
    val transactions: List<TransactionInfo>,
)
