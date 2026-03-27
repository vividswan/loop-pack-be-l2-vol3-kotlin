package com.loopers.domain.event

import java.time.ZonedDateTime
import java.util.UUID

data class KafkaEventMessage(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val aggregateId: String,
    val payload: Map<String, Any?>,
    val version: Long = 1L,
    val occurredAt: String = ZonedDateTime.now().toString(),
)
