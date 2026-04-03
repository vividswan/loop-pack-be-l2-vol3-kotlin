package com.loopers.domain.queue

interface QueueRepository {
    fun addToQueue(memberId: Long): Boolean

    fun getPosition(memberId: Long): Long?

    fun getTotalWaitingCount(): Long

    fun popFromQueue(count: Long): List<Long>

    fun removeFromQueue(memberId: Long)

    fun issueToken(memberId: Long, token: String, ttlSeconds: Long)

    fun getToken(memberId: Long): String?

    fun deleteToken(memberId: Long)
}
