package com.loopers.domain.payment

import com.loopers.support.error.ErrorCode

enum class PaymentErrorCode(override val code: String, override val message: String) : ErrorCode {
    NOT_FOUND("payment.not-found", "존재하지 않는 결제입니다."),
    ALREADY_PROCESSED("payment.already-processed", "이미 처리된 결제입니다."),
    ORDER_NOT_FOUND("payment.order-not-found", "결제할 주문을 찾을 수 없습니다."),
    PG_REQUEST_FAILED("payment.pg-request-failed", "결제 요청에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    PG_UNAVAILABLE("payment.pg-unavailable", "결제 시스템이 일시적으로 이용 불가합니다. 잠시 후 다시 시도해 주세요."),
    INVALID_TRANSACTION_KEY("payment.invalid-transaction-key", "SUCCESS 결제에 트랜잭션 키가 존재하지 않습니다."),
}
