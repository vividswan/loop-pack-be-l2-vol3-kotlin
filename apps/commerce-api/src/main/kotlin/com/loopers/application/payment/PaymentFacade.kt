package com.loopers.application.payment

import com.loopers.domain.payment.PaymentCommand
import com.loopers.domain.payment.PaymentErrorCode
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.infrastructure.payment.pg.PgCallbackPayload
import com.loopers.infrastructure.payment.pg.PgClientAdapter
import com.loopers.infrastructure.payment.pg.PgPaymentRequest
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val pgClientAdapter: PgClientAdapter,
    @Value("\${payment.callback-url}") private val callbackUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun pay(memberId: Long, command: PaymentCommand.RequestPayment): PaymentInfo {
        // PG 요청 전 PENDING 상태로 먼저 저장 (멘토 권고)
        val payment = paymentService.createPendingPayment(command)

        try {
            val pgResponse = pgClientAdapter.requestPayment(
                userId = memberId.toString(),
                request = PgPaymentRequest(
                    orderId = payment.pgOrderId,
                    cardType = payment.cardType,
                    cardNo = payment.cardNo,
                    amount = payment.amount.toString(),
                    callbackUrl = callbackUrl,
                ),
            )

            // PG가 즉시 FAILED 응답을 반환하는 경우
            if (pgResponse.status == "FAILED") {
                val confirmed = paymentService.confirmPayment(
                    PaymentCommand.ConfirmPayment(
                        pgOrderId = payment.pgOrderId,
                        pgTransactionKey = pgResponse.transactionKey,
                        status = PaymentStatus.FAILED,
                        failureReason = pgResponse.reason,
                    ),
                )
                return PaymentInfo.from(confirmed)
            }
        } catch (e: CallNotPermittedException) {
            // CB 오픈 상태 — PG에 요청 자체가 안 갔으므로 즉시 FAILED 확정
            paymentService.confirmPayment(
                PaymentCommand.ConfirmPayment(
                    pgOrderId = payment.pgOrderId,
                    pgTransactionKey = null,
                    status = PaymentStatus.FAILED,
                    failureReason = "결제 시스템 일시 이용 불가",
                ),
            )
            throw CoreException(ErrorType.BAD_REQUEST, PaymentErrorCode.PG_UNAVAILABLE)
        } catch (e: Exception) {
            // 네트워크 오류 등 — PENDING 유지, 스케줄러가 나중에 복구
            log.warn("PG 결제 요청 실패. pgOrderId={}, error={}", payment.pgOrderId, e.message)
            throw CoreException(ErrorType.BAD_REQUEST, PaymentErrorCode.PG_REQUEST_FAILED)
        }

        // 비동기 처리 중 (PENDING) — 콜백 또는 스케줄러가 최종 상태 반영
        return PaymentInfo.from(payment)
    }

    fun handleCallback(payload: PgCallbackPayload) {
        val status = when (payload.status) {
            "SUCCESS" -> PaymentStatus.SUCCESS
            else -> PaymentStatus.FAILED
        }
        if (status == PaymentStatus.SUCCESS && payload.transactionKey == null) {
            throw CoreException(ErrorType.BAD_REQUEST, PaymentErrorCode.INVALID_TRANSACTION_KEY)
        }
        paymentService.confirmPayment(
            PaymentCommand.ConfirmPayment(
                pgOrderId = payload.orderId,
                pgTransactionKey = payload.transactionKey,
                status = status,
                failureReason = payload.reason,
            ),
        )
    }

    fun recoverPendingPayments() {
        val pendingPayments = paymentService.findPendingPaymentsOlderThan(10)
        log.info("복구 대상 PENDING 결제 건수: {}", pendingPayments.size)

        pendingPayments.forEach { payment ->
            try {
                val pgResponse = pgClientAdapter.getPaymentByOrderId(
                    userId = payment.memberId.toString(),
                    orderId = payment.pgOrderId,
                )

                // 가장 최신 트랜잭션 기준으로 확정
                val latestTx = pgResponse.transactions.firstOrNull() ?: return@forEach

                val status = when (latestTx.status) {
                    "SUCCESS" -> PaymentStatus.SUCCESS
                    "FAILED" -> PaymentStatus.FAILED
                    else -> return@forEach // 아직 PENDING 처리 중
                }

                paymentService.confirmPayment(
                    PaymentCommand.ConfirmPayment(
                        pgOrderId = payment.pgOrderId,
                        pgTransactionKey = latestTx.transactionKey,
                        status = status,
                        failureReason = latestTx.reason,
                    ),
                )
                log.info("결제 복구 완료. pgOrderId={}, status={}", payment.pgOrderId, status)
            } catch (e: Exception) {
                log.error("결제 복구 실패. pgOrderId={}, error={}", payment.pgOrderId, e.message)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getPayment(id: Long): PaymentInfo {
        return PaymentInfo.from(paymentService.getPayment(id))
    }
}
