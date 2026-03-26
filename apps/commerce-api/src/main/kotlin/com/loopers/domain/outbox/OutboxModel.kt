package com.loopers.domain.outbox

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "outbox_event",
    indexes = [
        Index(name = "idx_outbox_published", columnList = "published, created_at"),
    ],
)
class OutboxModel internal constructor(
    aggregateType: String,
    aggregateId: String,
    eventType: String,
    payload: String,
    topic: String,
) : BaseEntity() {

    @Column(name = "aggregate_type", nullable = false, length = 50)
    var aggregateType: String = aggregateType
        protected set

    @Column(name = "aggregate_id", nullable = false, length = 100)
    var aggregateId: String = aggregateId
        protected set

    @Column(name = "event_type", nullable = false, length = 100)
    var eventType: String = eventType
        protected set

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    var payload: String = payload
        protected set

    @Column(name = "topic", nullable = false, length = 100)
    var topic: String = topic
        protected set

    @Column(name = "published", nullable = false)
    var published: Boolean = false
        protected set

    fun markPublished() {
        this.published = true
    }

    companion object {
        fun create(
            aggregateType: String,
            aggregateId: String,
            eventType: String,
            payload: String,
            topic: String,
        ): OutboxModel {
            return OutboxModel(
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                eventType = eventType,
                payload = payload,
                topic = topic,
            )
        }
    }
}
