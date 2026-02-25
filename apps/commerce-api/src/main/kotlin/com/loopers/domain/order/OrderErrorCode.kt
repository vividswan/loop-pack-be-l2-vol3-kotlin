package com.loopers.domain.order

import com.loopers.support.error.ErrorCode

enum class OrderErrorCode(override val code: String, override val message: String) : ErrorCode {
    ITEMS_EMPTY("order.items.empty", "주문 항목은 비어있을 수 없습니다."),
    QUANTITY_NOT_POSITIVE("order.quantity.not-positive", "주문 수량은 1 이상이어야 합니다."),
    PRICE_NEGATIVE("order.price.negative", "주문 항목의 가격은 0 이상이어야 합니다."),
    NOT_FOUND("order.not-found", "존재하지 않는 주문입니다."),
}
