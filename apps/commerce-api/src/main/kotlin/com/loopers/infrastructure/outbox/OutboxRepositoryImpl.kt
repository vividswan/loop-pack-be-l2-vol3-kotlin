package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxModel
import com.loopers.domain.outbox.OutboxRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class OutboxRepositoryImpl(
    private val outboxJpaRepository: OutboxJpaRepository,
) : OutboxRepository {

    override fun save(outbox: OutboxModel): OutboxModel {
        return outboxJpaRepository.save(outbox)
    }

    override fun findById(id: Long): OutboxModel? {
        return outboxJpaRepository.findById(id).orElse(null)
    }

    override fun findUnpublished(limit: Int): List<OutboxModel> {
        return outboxJpaRepository.findUnpublished(PageRequest.of(0, limit))
    }
}
