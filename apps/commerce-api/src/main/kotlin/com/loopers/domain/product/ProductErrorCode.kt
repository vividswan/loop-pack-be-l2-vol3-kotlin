package com.loopers.domain.product

import com.loopers.support.error.ErrorCode

enum class ProductErrorCode(override val code: String, override val message: String) : ErrorCode {
    NAME_EMPTY("product.name.empty", "상품 이름은 비어있을 수 없습니다."),
    PRICE_NEGATIVE("product.price.negative", "상품 가격은 0 이상이어야 합니다."),
    STOCK_NEGATIVE("product.stock.negative", "재고는 0 이상이어야 합니다."),
    STOCK_NOT_ENOUGH("product.stock.not-enough", "재고가 부족합니다."),
    NOT_FOUND("product.not-found", "존재하지 않는 상품입니다."),
}
