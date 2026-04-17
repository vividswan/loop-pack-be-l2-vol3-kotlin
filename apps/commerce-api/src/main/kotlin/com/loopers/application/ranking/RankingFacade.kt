package com.loopers.application.ranking

import com.loopers.domain.productrank.MvProductRankMonthlyModel
import com.loopers.domain.productrank.MvProductRankWeeklyModel
import com.loopers.domain.ranking.ProductRankMvRepository
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.domain.ranking.RankingService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Transactional(readOnly = true)
@Component
class RankingFacade(
    private val rankingService: RankingService,
    private val productRankMvRepository: ProductRankMvRepository,
) {
    fun getTopRankings(period: RankingPeriod, limit: Long): List<RankingInfo> {
        val today = LocalDate.now(ZONE_SEOUL)
        return when (period) {
            RankingPeriod.DAILY -> rankingService.getTopRankings(limit)
                .map { RankingInfo.from(it) }
            RankingPeriod.WEEKLY -> {
                val periodKey = MvProductRankWeeklyModel.periodKeyFrom(today)
                productRankMvRepository.getWeeklyRankings(periodKey, limit)
                    .map { RankingInfo.from(it) }
            }
            RankingPeriod.MONTHLY -> {
                val periodKey = MvProductRankMonthlyModel.periodKeyFrom(today)
                productRankMvRepository.getMonthlyRankings(periodKey, limit)
                    .map { RankingInfo.from(it) }
            }
        }
    }

    fun getProductRank(productId: Long): RankingInfo? {
        return rankingService.getProductRank(productId)
            ?.let { RankingInfo.from(it) }
    }

    companion object {
        private val ZONE_SEOUL = ZoneId.of("Asia/Seoul")
    }
}
