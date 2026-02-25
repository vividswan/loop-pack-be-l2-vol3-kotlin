package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
import org.springframework.stereotype.Component

@Component
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {

    override fun save(like: LikeModel): LikeModel {
        return likeJpaRepository.save(like)
    }

    override fun findByMemberIdAndProductId(memberId: Long, productId: Long): LikeModel? {
        return likeJpaRepository.findByMemberIdAndProductId(memberId, productId)
    }

    override fun existsByMemberIdAndProductId(memberId: Long, productId: Long): Boolean {
        return likeJpaRepository.existsByMemberIdAndProductId(memberId, productId)
    }

    override fun delete(like: LikeModel) {
        likeJpaRepository.delete(like)
    }
}
