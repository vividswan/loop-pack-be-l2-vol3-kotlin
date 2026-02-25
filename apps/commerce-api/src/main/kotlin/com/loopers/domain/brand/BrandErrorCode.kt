package com.loopers.domain.brand

import com.loopers.support.error.ErrorCode

enum class BrandErrorCode(override val code: String, override val message: String) : ErrorCode {
    NAME_EMPTY("brand.name.empty", "브랜드 이름은 비어있을 수 없습니다."),
    NOT_FOUND("brand.not-found", "존재하지 않는 브랜드입니다."),
}
