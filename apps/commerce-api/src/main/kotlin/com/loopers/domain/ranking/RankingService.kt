package com.loopers.domain.ranking

import org.springframework.stereotype.Component

@Component
class RankingService(
    private val rankingRepository: RankingRepository,
) {
    fun getTopRankings(limit: Long): List<RankingEntry> {
        return rankingRepository.getTopRankings(limit)
    }

    fun getProductRank(productId: Long): RankingEntry? {
        return rankingRepository.getProductRankEntry(productId)
    }
}
