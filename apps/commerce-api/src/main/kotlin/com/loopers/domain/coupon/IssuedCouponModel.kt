package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.ZonedDateTime

@Entity
@Table(
    name = "issued_coupon",
    indexes = [
        Index(name = "idx_issued_coupon_member_id", columnList = "member_id"),
        Index(name = "idx_issued_coupon_coupon_id", columnList = "coupon_id"),
    ],
)
class IssuedCouponModel internal constructor(
    memberId: Long,
    couponId: Long,
) : BaseEntity() {

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: CouponStatus = CouponStatus.AVAILABLE
        protected set

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    fun use() {
        if (status != CouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.NOT_AVAILABLE)
        }
        this.status = CouponStatus.USED
        this.usedAt = ZonedDateTime.now()
    }

    fun expire() {
        if (status == CouponStatus.AVAILABLE) {
            this.status = CouponStatus.EXPIRED
        }
    }

    companion object {
        fun create(
            memberId: Long,
            couponId: Long,
        ): IssuedCouponModel {
            return IssuedCouponModel(
                memberId = memberId,
                couponId = couponId,
            )
        }
    }
}
