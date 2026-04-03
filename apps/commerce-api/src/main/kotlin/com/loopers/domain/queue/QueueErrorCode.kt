package com.loopers.domain.queue

import com.loopers.support.error.ErrorCode

enum class QueueErrorCode(override val code: String, override val message: String) : ErrorCode {
    ALREADY_IN_QUEUE("queue.already-in-queue", "이미 대기열에 진입한 상태입니다."),
    NOT_IN_QUEUE("queue.not-in-queue", "대기열에 존재하지 않는 유저입니다."),
    INVALID_TOKEN("queue.invalid-token", "유효하지 않은 입장 토큰입니다."),
    TOKEN_EXPIRED("queue.token-expired", "입장 토큰이 만료되었습니다."),
    TOKEN_REQUIRED("queue.token-required", "주문을 위해 입장 토큰이 필요합니다."),
}
