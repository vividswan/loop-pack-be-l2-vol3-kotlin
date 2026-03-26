package com.loopers.domain.coupon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CouponIssueRequestModelTest {

    @DisplayName("쿠폰 발급 요청 생성 시 PENDING 상태이다.")
    @Test
    fun createdRequestIsPending() {
        // arrange & act
        val request = CouponIssueRequestModel.create(memberId = 1L, couponId = 10L)

        // assert
        assertThat(request.status).isEqualTo(CouponIssueStatus.PENDING)
        assertThat(request.failureReason).isNull()
    }

    @DisplayName("발급 성공 시 SUCCESS 상태로 변경된다.")
    @Test
    fun markSuccessChangesStatus() {
        // arrange
        val request = CouponIssueRequestModel.create(memberId = 1L, couponId = 10L)

        // act
        request.markSuccess()

        // assert
        assertThat(request.status).isEqualTo(CouponIssueStatus.SUCCESS)
    }

    @DisplayName("발급 실패 시 FAILED 상태와 실패 사유가 기록된다.")
    @Test
    fun markFailedChangesStatusWithReason() {
        // arrange
        val request = CouponIssueRequestModel.create(memberId = 1L, couponId = 10L)

        // act
        request.markFailed("쿠폰이 모두 소진되었습니다.")

        // assert
        assertThat(request.status).isEqualTo(CouponIssueStatus.FAILED)
        assertThat(request.failureReason).isEqualTo("쿠폰이 모두 소진되었습니다.")
    }
}
