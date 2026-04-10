package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEntry

data class RankingInfo(
    val productId: Long,
    val score: Double,
    val rank: Long,
) {
    companion object {
        fun from(entry: RankingEntry): RankingInfo {
            return RankingInfo(
                productId = entry.productId,
                score = entry.score,
                rank = entry.rank,
            )
        }
    }
}
