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
    /**
     * 좋아요를 저장한다.
     *
     * 좋아요 수 집계(product.likeCount)는 이벤트 기반으로 비동기 처리한다.
     * 집계 실패와 무관하게 좋아요 자체는 성공한다. (Eventual Consistency)
     *
     * 비관적 락을 제거한 이유:
     * - likeCount 업데이트가 동기에서 비동기로 분리되어 경합 포인트가 사라짐
     * - 중복 좋아요 방지는 (member_id, product_id) unique 제약으로 보장
     */
    fun like(memberId: Long, productId: Long): LikeModel {
        productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, ProductErrorCode.NOT_FOUND)

        if (likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            throw CoreException(ErrorType.CONFLICT, LikeErrorCode.ALREADY_LIKED)
        }

        val like = LikeModel.create(memberId = memberId, productId = productId)
        return likeRepository.save(like)
    }

    fun unlike(memberId: Long, productId: Long) {
        productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, ProductErrorCode.NOT_FOUND)

        val like = likeRepository.findByMemberIdAndProductId(memberId, productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, LikeErrorCode.NOT_FOUND)

        likeRepository.delete(like)
    }
}
