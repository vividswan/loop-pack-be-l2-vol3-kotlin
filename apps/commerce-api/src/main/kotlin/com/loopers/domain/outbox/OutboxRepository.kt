package com.loopers.domain.outbox

interface OutboxRepository {
    fun save(outbox: OutboxModel): OutboxModel
    fun findById(id: Long): OutboxModel?
    fun findUnpublished(limit: Int): List<OutboxModel>
}
