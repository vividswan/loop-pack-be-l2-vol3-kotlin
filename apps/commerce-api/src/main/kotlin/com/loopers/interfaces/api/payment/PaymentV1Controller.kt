package com.loopers.interfaces.api.payment

import com.loopers.application.member.MemberInfo
import com.loopers.application.payment.PaymentFacade
import com.loopers.infrastructure.payment.pg.PgCallbackPayload
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentV1Controller(
    private val paymentFacade: PaymentFacade,
) : PaymentV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun pay(
        @AuthenticatedMember memberInfo: MemberInfo,
        @RequestBody request: PaymentV1Dto.PayRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        return paymentFacade.pay(memberInfo.id, request.toCommand(memberInfo.id))
            .let { PaymentV1Dto.PaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/callback")
    override fun callback(
        @RequestBody payload: PgCallbackPayload,
    ): ApiResponse<Unit> {
        paymentFacade.handleCallback(payload)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/{id}")
    override fun getPayment(
        @AuthenticatedMember memberInfo: MemberInfo,
        @PathVariable id: Long,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        return paymentFacade.getPayment(id)
            .let { PaymentV1Dto.PaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/recover")
    override fun recover(
        @AuthenticatedMember memberInfo: MemberInfo,
    ): ApiResponse<Unit> {
        paymentFacade.recoverPendingPayments()
        return ApiResponse.success(Unit)
    }
}
