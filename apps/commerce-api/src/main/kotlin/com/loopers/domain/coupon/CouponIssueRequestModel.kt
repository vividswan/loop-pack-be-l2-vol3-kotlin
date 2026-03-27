package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "coupon_issue_request",
    indexes = [
        Index(name = "idx_coupon_issue_request_member_coupon", columnList = "member_id, coupon_id"),
    ],
)
class CouponIssueRequestModel internal constructor(
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
    var status: CouponIssueStatus = CouponIssueStatus.PENDING
        protected set

    @Column(name = "failure_reason")
    var failureReason: String? = null
        protected set

    fun markSuccess() {
        this.status = CouponIssueStatus.SUCCESS
    }

    fun markFailed(reason: String) {
        this.status = CouponIssueStatus.FAILED
        this.failureReason = reason
    }

    companion object {
        fun create(memberId: Long, couponId: Long): CouponIssueRequestModel {
            return CouponIssueRequestModel(memberId = memberId, couponId = couponId)
        }
    }
}
