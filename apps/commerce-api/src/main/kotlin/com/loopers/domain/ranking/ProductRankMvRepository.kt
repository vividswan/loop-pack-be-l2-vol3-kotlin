package com.loopers.domain.ranking

interface ProductRankMvRepository {

    fun getWeeklyRankings(periodKey: String, limit: Long): List<RankingEntry>

    fun getMonthlyRankings(periodKey: String, limit: Long): List<RankingEntry>
}
