package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupon")
class CouponModel internal constructor(
    name: String,
    type: CouponType,
    value: Long,
    minOrderAmount: Long,
    expiredAt: ZonedDateTime,
) : BaseEntity() {

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: CouponType = type
        protected set

    @Column(name = "value", nullable = false)
    var value: Long = value
        protected set

    @Column(name = "min_order_amount", nullable = false)
    var minOrderAmount: Long = minOrderAmount
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    init {
        validateName(name)
        validateValue(value, type)
        validateMinOrderAmount(minOrderAmount)
    }

    fun update(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long,
        expiredAt: ZonedDateTime,
    ) {
        validateName(name)
        validateValue(value, type)
        validateMinOrderAmount(minOrderAmount)
        this.name = name
        this.type = type
        this.value = value
        this.minOrderAmount = minOrderAmount
        this.expiredAt = expiredAt
    }

    fun isExpired(): Boolean {
        return ZonedDateTime.now().isAfter(expiredAt)
    }

    fun calculateDiscount(orderAmount: Long): Long {
        if (isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.EXPIRED)
        }
        if (orderAmount < minOrderAmount) {
            throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.MIN_ORDER_AMOUNT_NOT_MET)
        }

        return when (type) {
            CouponType.FIXED -> minOf(value, orderAmount)
            CouponType.RATE -> orderAmount * value / 100
        }
    }

    companion object {
        fun create(
            name: String,
            type: CouponType,
            value: Long,
            minOrderAmount: Long,
            expiredAt: ZonedDateTime,
        ): CouponModel {
            return CouponModel(
                name = name,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            )
        }

        private fun validateName(name: String) {
            if (name.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.NAME_EMPTY)
            }
        }

        private fun validateValue(value: Long, type: CouponType) {
            if (value < 1) {
                throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.VALUE_NOT_POSITIVE)
            }
            if (type == CouponType.RATE && value > 100) {
                throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.RATE_EXCEEDS_100)
            }
        }

        private fun validateMinOrderAmount(minOrderAmount: Long) {
            if (minOrderAmount < 0) {
                throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.MIN_ORDER_AMOUNT_NEGATIVE)
            }
        }
    }
}
