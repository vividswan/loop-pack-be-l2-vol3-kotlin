package com.loopers.interfaces.api.payment

import com.loopers.application.member.MemberInfo
import com.loopers.infrastructure.payment.pg.PgCallbackPayload
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Payment", description = "결제 API")
interface PaymentV1ApiSpec {

    @Operation(summary = "결제 요청", description = "주문에 대한 카드 결제를 요청합니다.")
    fun pay(
        @AuthenticatedMember memberInfo: MemberInfo,
        request: PaymentV1Dto.PayRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(summary = "결제 콜백", description = "PG 시스템으로부터 결제 처리 결과를 수신합니다.")
    fun callback(
        payload: PgCallbackPayload,
    ): ApiResponse<Unit>

    @Operation(summary = "결제 조회", description = "결제 정보를 조회합니다.")
    fun getPayment(
        @AuthenticatedMember memberInfo: MemberInfo,
        id: Long,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(summary = "결제 수동 복구", description = "PENDING 상태 결제건을 PG 조회로 수동 복구합니다.")
    fun recover(
        @AuthenticatedMember memberInfo: MemberInfo,
    ): ApiResponse<Unit>
}
