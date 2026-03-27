package com.loopers.domain.coupon

import com.loopers.support.error.ErrorCode

enum class CouponErrorCode(override val code: String, override val message: String) : ErrorCode {
    NOT_FOUND("coupon.not-found", "존재하지 않는 쿠폰입니다."),
    NAME_EMPTY("coupon.name.empty", "쿠폰 이름은 비어있을 수 없습니다."),
    VALUE_NOT_POSITIVE("coupon.value.not-positive", "쿠폰 할인 값은 1 이상이어야 합니다."),
    RATE_EXCEEDS_100("coupon.rate.exceeds-100", "정률 할인은 100을 초과할 수 없습니다."),
    MIN_ORDER_AMOUNT_NEGATIVE("coupon.min-order-amount.negative", "최소 주문금액은 0 이상이어야 합니다."),
    EXPIRED("coupon.expired", "만료된 쿠폰입니다."),
    MIN_ORDER_AMOUNT_NOT_MET("coupon.min-order-amount.not-met", "최소 주문금액을 충족하지 않습니다."),
    ISSUED_NOT_FOUND("coupon.issued.not-found", "발급된 쿠폰을 찾을 수 없습니다."),
    ALREADY_USED("coupon.already-used", "이미 사용된 쿠폰입니다."),
    NOT_AVAILABLE("coupon.not-available", "사용 가능한 상태의 쿠폰이 아닙니다."),
    ALREADY_ISSUED("coupon.already-issued", "이미 발급받은 쿠폰입니다."),
    OWNER_MISMATCH("coupon.owner-mismatch", "본인 소유의 쿠폰이 아닙니다."),
    ISSUE_REQUEST_NOT_FOUND("coupon.issue-request.not-found", "쿠폰 발급 요청을 찾을 수 없습니다."),
    SOLD_OUT("coupon.sold-out", "선착순 쿠폰이 모두 소진되었습니다."),
    NOT_FIRST_COME_COUPON("coupon.not-first-come", "선착순 쿠폰이 아닙니다."),
}
