package com.loopers.application.like

data class LikeAggregateEvent(
    val productId: Long,
    val increment: Boolean,
)
