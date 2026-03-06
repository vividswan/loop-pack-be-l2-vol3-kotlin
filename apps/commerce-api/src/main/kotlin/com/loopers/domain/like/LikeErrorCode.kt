package com.loopers.domain.like

import com.loopers.support.error.ErrorCode

enum class LikeErrorCode(override val code: String, override val message: String) : ErrorCode {
    ALREADY_LIKED("like.already-liked", "이미 좋아요한 상품입니다."),
    NOT_FOUND("like.not-found", "좋아요 기록이 존재하지 않습니다."),
}
