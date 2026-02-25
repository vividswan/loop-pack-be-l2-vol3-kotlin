package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeInfo

class LikeV1Dto {
    data class LikeResponse(
        val id: Long,
        val memberId: Long,
        val productId: Long,
    ) {
        companion object {
            fun from(info: LikeInfo): LikeResponse {
                return LikeResponse(
                    id = info.id,
                    memberId = info.memberId,
                    productId = info.productId,
                )
            }
        }
    }
}
