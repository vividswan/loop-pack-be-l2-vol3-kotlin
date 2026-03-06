package com.loopers.application.like

import com.loopers.domain.like.LikeModel

data class LikeInfo(
    val id: Long,
    val memberId: Long,
    val productId: Long,
) {
    companion object {
        fun from(model: LikeModel): LikeInfo {
            return LikeInfo(
                id = model.id,
                memberId = model.memberId,
                productId = model.productId,
            )
        }
    }
}
