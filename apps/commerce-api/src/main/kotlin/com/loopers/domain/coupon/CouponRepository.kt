package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponRepository {
    fun save(coupon: CouponModel): CouponModel
    fun findById(id: Long): CouponModel?
    fun findAll(pageable: Pageable): Page<CouponModel>
    fun delete(coupon: CouponModel)
}
