package com.loopers.domain.payment

enum class PaymentStatus {
    /** PG에 요청했으나 처리 결과를 아직 받지 못한 상태 */
    PENDING,

    /** 결제 처리 성공 */
    SUCCESS,

    /** 결제 처리 실패 (한도 초과, 잘못된 카드 등) */
    FAILED,

    /** 결제 취소 */
    CANCELLED,
}
