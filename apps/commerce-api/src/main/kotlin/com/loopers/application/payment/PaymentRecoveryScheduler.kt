package com.loopers.application.payment

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PaymentRecoveryScheduler(
    private val paymentFacade: PaymentFacade,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 1분마다 PENDING 결제건을 PG에서 조회하여 상태 복구 */
    @Scheduled(fixedDelay = 60_000)
    fun recoverPendingPayments() {
        log.info("결제 상태 복구 스케줄러 실행")
        paymentFacade.recoverPendingPayments()
    }
}
