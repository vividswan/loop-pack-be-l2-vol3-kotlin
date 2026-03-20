package com.loopers.infrastructure.payment.pg

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component

@Component
class PgClientAdapter(
    private val pgClient: PgClient,
) {
    @CircuitBreaker(name = "pg")
    @Retry(name = "pg")
    fun requestPayment(userId: String, request: PgPaymentRequest): PgPaymentResponse {
        return pgClient.requestPayment(userId, request)
    }

    @CircuitBreaker(name = "pg")
    fun getPaymentByOrderId(userId: String, orderId: String): PgOrderPaymentResponse {
        return pgClient.getPaymentByOrderId(userId, orderId)
    }
}
