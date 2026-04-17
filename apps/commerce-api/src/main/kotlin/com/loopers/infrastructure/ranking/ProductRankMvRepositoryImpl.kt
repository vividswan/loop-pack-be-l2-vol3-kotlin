package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankMvRepository
import com.loopers.domain.ranking.RankingEntry
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProductRankMvRepositoryImpl(
    private val weeklyJpaRepository: MvProductRankWeeklyJpaRepository,
    private val monthlyJpaRepository: MvProductRankMonthlyJpaRepository,
) : ProductRankMvRepository {

    override fun getWeeklyRankings(periodKey: String, limit: Long): List<RankingEntry> {
        return weeklyJpaRepository.findByPeriodKeyOrderByRankPositionAsc(
            periodKey,
            PageRequest.of(0, limit.toInt()),
        ).map {
            RankingEntry(
                productId = it.productId,
                score = it.score,
                rank = it.rankPosition.toLong(),
            )
        }
    }

    override fun getMonthlyRankings(periodKey: String, limit: Long): List<RankingEntry> {
        return monthlyJpaRepository.findByPeriodKeyOrderByRankPositionAsc(
            periodKey,
            PageRequest.of(0, limit.toInt()),
        ).map {
            RankingEntry(
                productId = it.productId,
                score = it.score,
                rank = it.rankPosition.toLong(),
            )
        }
    }
}
