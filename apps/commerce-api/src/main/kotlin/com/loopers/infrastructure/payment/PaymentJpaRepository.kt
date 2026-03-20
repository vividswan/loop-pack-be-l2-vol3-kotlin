package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentModel
import com.loopers.domain.payment.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface PaymentJpaRepository : JpaRepository<PaymentModel, Long> {
    fun findByPgOrderId(pgOrderId: String): PaymentModel?
    fun findByOrderId(orderId: Long): PaymentModel?

    @Query(
        "SELECT p FROM PaymentModel p WHERE p.status = :status AND p.createdAt < :threshold",
    )
    fun findByStatusAndCreatedAtBefore(
        @Param("status") status: PaymentStatus,
        @Param("threshold") threshold: ZonedDateTime,
    ): List<PaymentModel>
}
