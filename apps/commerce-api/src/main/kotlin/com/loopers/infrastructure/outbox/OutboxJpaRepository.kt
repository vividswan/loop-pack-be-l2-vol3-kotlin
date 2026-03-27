package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OutboxJpaRepository : JpaRepository<OutboxModel, Long> {

    @Query("SELECT o FROM OutboxModel o WHERE o.published = false ORDER BY o.createdAt ASC")
    fun findUnpublished(pageable: Pageable): List<OutboxModel>
}
