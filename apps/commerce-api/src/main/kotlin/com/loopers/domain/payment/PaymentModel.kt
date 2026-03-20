package com.loopers.domain.payment

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payment_order_id", columnList = "order_id"),
        Index(name = "idx_payment_member_id", columnList = "member_id"),
        Index(name = "idx_payment_status", columnList = "status"),
    ],
)
class PaymentModel internal constructor(
    orderId: Long,
    memberId: Long,
    pgOrderId: String,
    cardType: String,
    cardNo: String,
    amount: Long,
) : BaseEntity() {

    @Column(name = "order_id", nullable = false)
    var orderId: Long = orderId
        protected set

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    /** PG에 전달하는 주문 식별자 (내부 orderId를 포맷팅한 값) */
    @Column(name = "pg_order_id", nullable = false, unique = true)
    var pgOrderId: String = pgOrderId
        protected set

    @Column(name = "card_type", nullable = false)
    var cardType: String = cardType
        protected set

    @Column(name = "card_no", nullable = false)
    var cardNo: String = cardNo
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = amount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus = PaymentStatus.PENDING
        protected set

    /** PG로부터 받은 트랜잭션 키 */
    @Column(name = "pg_transaction_key")
    var pgTransactionKey: String? = null
        protected set

    @Column(name = "failure_reason")
    var failureReason: String? = null
        protected set

    fun confirm(pgTransactionKey: String?, status: PaymentStatus, failureReason: String?) {
        if (this.status != PaymentStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, PaymentErrorCode.ALREADY_PROCESSED)
        }
        if (status == PaymentStatus.SUCCESS && pgTransactionKey == null) {
            throw CoreException(ErrorType.BAD_REQUEST, PaymentErrorCode.INVALID_TRANSACTION_KEY)
        }
        this.pgTransactionKey = pgTransactionKey
        this.status = status
        this.failureReason = failureReason
    }

    companion object {
        fun create(
            orderId: Long,
            memberId: Long,
            cardType: String,
            cardNo: String,
            amount: Long,
        ): PaymentModel {
            val pgOrderId = "ORDER-%010d".format(orderId)
            return PaymentModel(
                orderId = orderId,
                memberId = memberId,
                pgOrderId = pgOrderId,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
            )
        }
    }
}
