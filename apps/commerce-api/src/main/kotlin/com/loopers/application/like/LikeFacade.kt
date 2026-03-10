package com.loopers.application.like

import com.loopers.domain.like.LikeService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun like(memberId: Long, productId: Long): LikeInfo {
        val like = likeService.like(memberId, productId)
        eventPublisher.publishEvent(LikeCacheEvictEvent(like.productId))
        return LikeInfo.from(like)
    }

    @Transactional
    fun unlike(memberId: Long, productId: Long) {
        likeService.unlike(memberId, productId)
        eventPublisher.publishEvent(LikeCacheEvictEvent(productId))
    }
}
