package com.loopers.application.event

data class UserActionEvent(
    val memberId: Long,
    val action: String,
    val targetType: String,
    val targetId: Long,
    val metadata: Map<String, Any?> = emptyMap(),
)
