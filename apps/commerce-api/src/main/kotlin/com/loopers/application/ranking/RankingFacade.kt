package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingService
import org.springframework.stereotype.Component

@Component
class RankingFacade(
    private val rankingService: RankingService,
) {
    fun getTopRankings(limit: Long): List<RankingInfo> {
        return rankingService.getTopRankings(limit)
            .map { RankingInfo.from(it) }
    }

    fun getProductRank(productId: Long): RankingInfo? {
        return rankingService.getProductRank(productId)
            ?.let { RankingInfo.from(it) }
    }
}
