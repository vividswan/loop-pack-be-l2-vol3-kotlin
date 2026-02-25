package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import org.springframework.data.jpa.repository.JpaRepository

interface LikeJpaRepository : JpaRepository<LikeModel, Long> {
    fun findByMemberIdAndProductId(memberId: Long, productId: Long): LikeModel?
    fun existsByMemberIdAndProductId(memberId: Long, productId: Long): Boolean
}
