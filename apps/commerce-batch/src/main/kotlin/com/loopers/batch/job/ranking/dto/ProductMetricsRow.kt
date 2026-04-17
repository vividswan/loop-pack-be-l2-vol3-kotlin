package com.loopers.batch.job.ranking.dto

data class ProductMetricsRow(
    val productId: Long,
    val likeCount: Long,
    val viewCount: Long,
    val orderCount: Long,
    val salesAmount: Long,
    val score: Double,
)
