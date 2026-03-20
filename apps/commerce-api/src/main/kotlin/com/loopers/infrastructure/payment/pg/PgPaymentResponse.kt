package com.loopers.infrastructure.payment.pg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** PG 결제 요청 응답 (비동기 요청 접수 결과) — transactionKey, status, reason 반환 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PgPaymentResponse(
    val transactionKey: String?,
    val status: String,
    val reason: String? = null,
)

/** PG 결제 처리 결과 콜백 페이로드 — TransactionInfo 구조 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PgCallbackPayload(
    val transactionKey: String?,
    val orderId: String,
    val status: String,
    val reason: String? = null,
)

/** PG 주문 조회 응답 — orderId + transactions 목록 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PgOrderPaymentResponse(
    val orderId: String,
    val transactions: List<PgTransactionItem> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PgTransactionItem(
    val transactionKey: String?,
    val status: String,
    val reason: String? = null,
)
