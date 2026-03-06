package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun save(coupon: CouponModel): CouponModel {
        return couponJpaRepository.save(coupon)
    }

    override fun findById(id: Long): CouponModel? {
        return couponJpaRepository.findById(id).orElse(null)
    }

    override fun findAll(pageable: Pageable): Page<CouponModel> {
        return couponJpaRepository.findAll(pageable)
    }

    override fun delete(coupon: CouponModel) {
        couponJpaRepository.delete(coupon)
    }
}
