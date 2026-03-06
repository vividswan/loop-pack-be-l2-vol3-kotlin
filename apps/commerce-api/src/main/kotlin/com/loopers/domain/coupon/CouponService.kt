package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponService(
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    fun createCoupon(coupon: CouponModel): CouponModel {
        return couponRepository.save(coupon)
    }

    fun getCoupon(id: Long): CouponModel {
        return couponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, CouponErrorCode.NOT_FOUND)
    }

    fun getCoupons(pageable: Pageable): Page<CouponModel> {
        return couponRepository.findAll(pageable)
    }

    fun updateCoupon(
        id: Long,
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long,
        expiredAt: ZonedDateTime,
    ): CouponModel {
        val coupon = getCoupon(id)
        coupon.update(name, type, value, minOrderAmount, expiredAt)
        return couponRepository.save(coupon)
    }

    fun deleteCoupon(id: Long) {
        val coupon = getCoupon(id)
        couponRepository.delete(coupon)
    }

    fun issueCoupon(memberId: Long, couponId: Long): IssuedCouponModel {
        val coupon = getCoupon(couponId)

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.EXPIRED)
        }

        if (issuedCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw CoreException(ErrorType.CONFLICT, CouponErrorCode.ALREADY_ISSUED)
        }

        val issuedCoupon = IssuedCouponModel.create(memberId = memberId, couponId = couponId)
        return issuedCouponRepository.save(issuedCoupon)
    }

    fun useIssuedCoupon(issuedCouponId: Long): IssuedCouponModel {
        val issuedCoupon = issuedCouponRepository.findById(issuedCouponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, CouponErrorCode.ISSUED_NOT_FOUND)

        val coupon = getCoupon(issuedCoupon.couponId)

        if (coupon.isExpired()) {
            throw CoreException(ErrorType.BAD_REQUEST, CouponErrorCode.EXPIRED)
        }

        issuedCoupon.use()
        return issuedCouponRepository.save(issuedCoupon)
    }

    fun getIssuedCoupon(id: Long): IssuedCouponModel {
        return issuedCouponRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, CouponErrorCode.ISSUED_NOT_FOUND)
    }

    fun getMyIssuedCoupons(memberId: Long): List<IssuedCouponModel> {
        return issuedCouponRepository.findAllByMemberId(memberId)
    }

    fun getMyAvailableCoupons(memberId: Long): List<IssuedCouponModel> {
        return issuedCouponRepository.findAllByMemberIdAndStatus(memberId, CouponStatus.AVAILABLE)
    }

    fun getIssuedCouponsByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCouponModel> {
        return issuedCouponRepository.findAllByCouponId(couponId, pageable)
    }
}
