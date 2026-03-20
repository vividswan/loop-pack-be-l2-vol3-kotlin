package com.loopers.infrastructure.payment.pg

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "pg-client",
    url = "\${pg.simulator.url}",
    configuration = [PgClientConfig::class],
)
interface PgClient {

    @PostMapping("/api/v1/payments")
    fun requestPayment(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestBody request: PgPaymentRequest,
    ): PgPaymentResponse

    @GetMapping("/api/v1/payments")
    fun getPaymentByOrderId(
        @RequestHeader("X-USER-ID") userId: String,
        @RequestParam("orderId") orderId: String,
    ): PgOrderPaymentResponse
}
