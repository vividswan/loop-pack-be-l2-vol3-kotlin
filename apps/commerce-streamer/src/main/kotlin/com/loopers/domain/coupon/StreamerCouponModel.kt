package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * commerce-streamer 전용 쿠폰 엔티티.
 *
 * commerce-api의 CouponModel과 같은 테이블을 참조하지만,
 * streamer가 필요한 필드만 노출한다.
 * 멀티모듈에서 엔티티를 공유하면 불필요한 의존이 발생하고,
 * 각 모듈의 변경이 서로에게 전파되므로 별도 정의한다.
 */
@Entity
@Table(name = "coupon")
class StreamerCouponModel : BaseEntity() {

    @Column(name = "name", nullable = false)
    var name: String = ""
        protected set

    @Column(name = "max_issuance_count")
    var maxIssuanceCount: Int? = null
        protected set
}
