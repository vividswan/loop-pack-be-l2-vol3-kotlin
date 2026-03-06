package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.IssuedCouponModel
import com.loopers.domain.coupon.IssuedCouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {

    override fun save(issuedCoupon: IssuedCouponModel): IssuedCouponModel {
        return issuedCouponJpaRepository.save(issuedCoupon)
    }

    override fun findById(id: Long): IssuedCouponModel? {
        return issuedCouponJpaRepository.findById(id).orElse(null)
    }

    override fun findByMemberIdAndCouponId(memberId: Long, couponId: Long): IssuedCouponModel? {
        return issuedCouponJpaRepository.findByMemberIdAndCouponId(memberId, couponId)
    }

    override fun existsByMemberIdAndCouponId(memberId: Long, couponId: Long): Boolean {
        return issuedCouponJpaRepository.existsByMemberIdAndCouponId(memberId, couponId)
    }

    override fun findAllByMemberId(memberId: Long): List<IssuedCouponModel> {
        return issuedCouponJpaRepository.findAllByMemberId(memberId)
    }

    override fun findAllByMemberIdAndStatus(memberId: Long, status: CouponStatus): List<IssuedCouponModel> {
        return issuedCouponJpaRepository.findAllByMemberIdAndStatus(memberId, status)
    }

    override fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCouponModel> {
        return issuedCouponJpaRepository.findAllByCouponId(couponId, pageable)
    }
}
