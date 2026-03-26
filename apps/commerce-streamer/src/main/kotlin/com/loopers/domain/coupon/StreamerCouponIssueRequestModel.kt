package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * commerce-streamer 전용 쿠폰 발급 요청 엔티티.
 */
@Entity
@Table(name = "coupon_issue_request")
class StreamerCouponIssueRequestModel : BaseEntity() {

    @Column(name = "member_id", nullable = false)
    var memberId: Long = 0L
        protected set

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = 0L
        protected set

    @Column(name = "status", nullable = false)
    var status: String = "PENDING"
        protected set

    @Column(name = "failure_reason")
    var failureReason: String? = null
        protected set

    fun markSuccess() {
        this.status = "SUCCESS"
    }

    fun markFailed(reason: String) {
        this.status = "FAILED"
        this.failureReason = reason
    }
}
