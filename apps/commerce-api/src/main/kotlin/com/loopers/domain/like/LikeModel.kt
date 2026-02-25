package com.loopers.domain.like

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "product_like",
    uniqueConstraints = [UniqueConstraint(name = "uk_like_member_product", columnNames = ["member_id", "product_id"])],
    indexes = [
        Index(name = "idx_like_member_id", columnList = "member_id"),
        Index(name = "idx_like_product_id", columnList = "product_id"),
    ],
)
class LikeModel internal constructor(
    memberId: Long,
    productId: Long,
) : BaseEntity() {

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    companion object {
        fun create(
            memberId: Long,
            productId: Long,
        ): LikeModel {
            return LikeModel(
                memberId = memberId,
                productId = productId,
            )
        }
    }
}
