package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingInfo

class RankingV1Dto {
    data class RankingItemResponse(
        val productId: Long,
        val score: Double,
        val rank: Long,
    ) {
        companion object {
            fun from(info: RankingInfo): RankingItemResponse {
                return RankingItemResponse(
                    productId = info.productId,
                    score = info.score,
                    rank = info.rank,
                )
            }
        }
    }

    data class TopRankingsResponse(
        val rankings: List<RankingItemResponse>,
    ) {
        companion object {
            fun from(infos: List<RankingInfo>): TopRankingsResponse {
                return TopRankingsResponse(
                    rankings = infos.map { RankingItemResponse.from(it) },
                )
            }
        }
    }

    data class ProductRankResponse(
        val productId: Long,
        val score: Double,
        val rank: Long,
    ) {
        companion object {
            fun from(info: RankingInfo): ProductRankResponse {
                return ProductRankResponse(
                    productId = info.productId,
                    score = info.score,
                    rank = info.rank,
                )
            }
        }
    }
}
