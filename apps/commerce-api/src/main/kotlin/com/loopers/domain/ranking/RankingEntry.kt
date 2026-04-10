package com.loopers.domain.ranking

data class RankingEntry(
    val productId: Long,
    val score: Double,
    val rank: Long,
)
