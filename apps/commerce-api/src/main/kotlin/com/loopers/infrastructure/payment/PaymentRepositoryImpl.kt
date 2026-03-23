package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PaymentRepositoryImpl(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {

    override fun save(payment: PaymentModel): PaymentModel {
        return paymentJpaRepository.save(payment)
    }

    override fun findById(id: Long): PaymentModel? {
        return paymentJpaRepository.findById(id).orElse(null)
    }

    override fun findByPgOrderId(pgOrderId: String): PaymentModel? {
        return paymentJpaRepository.findByPgOrderId(pgOrderId)
    }

    override fun findByOrderId(orderId: Long): PaymentModel? {
        return paymentJpaRepository.findByOrderId(orderId)
    }

    override fun findPendingOlderThan(threshold: ZonedDateTime): List<PaymentModel> {
        return paymentJpaRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, threshold)
    }
}
