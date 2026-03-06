package com.loopers.domain.like

interface LikeRepository {
    fun save(like: LikeModel): LikeModel
    fun findByMemberIdAndProductId(memberId: Long, productId: Long): LikeModel?
    fun existsByMemberIdAndProductId(memberId: Long, productId: Long): Boolean
    fun delete(like: LikeModel)
}
