package com.loopers.domain.like

import com.loopers.domain.product.ProductErrorCode
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class LikeService(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    fun like(memberId: Long, productId: Long): LikeModel {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, ProductErrorCode.NOT_FOUND)

        if (likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            throw CoreException(ErrorType.CONFLICT, LikeErrorCode.ALREADY_LIKED)
        }

        val like = LikeModel.create(memberId = memberId, productId = productId)
        val savedLike = likeRepository.save(like)
        product.increaseLikeCount()
        return savedLike
    }

    fun unlike(memberId: Long, productId: Long) {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, ProductErrorCode.NOT_FOUND)

        val like = likeRepository.findByMemberIdAndProductId(memberId, productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, LikeErrorCode.NOT_FOUND)

        likeRepository.delete(like)
        product.decreaseLikeCount()
    }
}
