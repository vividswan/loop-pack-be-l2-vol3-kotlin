package com.loopers.domain.payment

import java.time.ZonedDateTime

interface PaymentRepository {
    fun save(payment: PaymentModel): PaymentModel
    fun findById(id: Long): PaymentModel?
    fun findByPgOrderId(pgOrderId: String): PaymentModel?
    fun findByOrderId(orderId: Long): PaymentModel?
    fun findPendingOlderThan(threshold: ZonedDateTime): List<PaymentModel>
}
