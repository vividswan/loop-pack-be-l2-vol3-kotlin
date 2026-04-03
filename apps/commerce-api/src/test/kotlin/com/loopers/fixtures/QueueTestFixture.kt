package com.loopers.fixtures

import com.loopers.domain.queue.QueueRepository
import java.util.UUID

object QueueTestFixture {

    const val DEFAULT_TOKEN_TTL = 300L

    fun issueTestToken(queueRepository: QueueRepository, memberId: Long): String {
        val token = UUID.randomUUID().toString()
        queueRepository.issueToken(memberId, token, DEFAULT_TOKEN_TTL)
        return token
    }
}
