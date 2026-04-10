package com.loopers.domain.ranking

interface RankingRepository {
    fun getTopRankings(limit: Long): List<RankingEntry>

    fun getProductRankEntry(productId: Long): RankingEntry?

    fun carryOverFromYesterday(weight: Double)
}
