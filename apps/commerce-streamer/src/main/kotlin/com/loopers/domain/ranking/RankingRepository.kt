package com.loopers.domain.ranking

interface RankingRepository {
    fun incrementScore(productId: Long, score: Double)
}
