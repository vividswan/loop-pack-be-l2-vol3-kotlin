package com.loopers.application.like

import com.loopers.domain.like.LikeService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
) {
    @Transactional
    fun like(memberId: Long, productId: Long): LikeInfo {
        val like = likeService.like(memberId, productId)
        return LikeInfo.from(like)
    }

    @Transactional
    fun unlike(memberId: Long, productId: Long) {
        likeService.unlike(memberId, productId)
    }
}
