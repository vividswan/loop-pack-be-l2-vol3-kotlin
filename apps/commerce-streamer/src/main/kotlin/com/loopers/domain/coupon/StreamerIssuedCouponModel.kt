package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

/**
 * commerce-streamer 전용 발급 쿠폰 엔티티.
 */
@Entity
@Table(name = "issued_coupon")
class StreamerIssuedCouponModel : BaseEntity() {

    @Column(name = "member_id", nullable = false)
    var memberId: Long = 0L
        protected set

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = 0L
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: String = "AVAILABLE"
        protected set

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    companion object {
        fun create(memberId: Long, couponId: Long): StreamerIssuedCouponModel {
            return StreamerIssuedCouponModel().apply {
                this.memberId = memberId
                this.couponId = couponId
            }
        }
    }
}
