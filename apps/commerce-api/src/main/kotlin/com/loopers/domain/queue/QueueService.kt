package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QueueService(
    private val queueRepository: QueueRepository,
) {
    companion object {
        const val TOKEN_TTL_SECONDS = 300L
        const val BATCH_SIZE = 18L
        const val THROUGHPUT_PER_SECOND = 175.0
    }

    fun enterQueue(memberId: Long): Long {
        val added = queueRepository.addToQueue(memberId)
        if (!added) {
            throw CoreException(ErrorType.CONFLICT, QueueErrorCode.ALREADY_IN_QUEUE)
        }
        return queueRepository.getPosition(memberId)
            ?: throw CoreException(ErrorType.INTERNAL_ERROR)
    }

    fun getQueueStatus(memberId: Long): QueueStatus {
        val token = queueRepository.getToken(memberId)
        if (token != null) {
            return QueueStatus.ready(token)
        }

        val position = queueRepository.getPosition(memberId)
            ?: return QueueStatus.notInQueue()

        val estimatedWaitSeconds = calculateEstimatedWaitSeconds(position)
        return QueueStatus.waiting(position, estimatedWaitSeconds)
    }

    fun getTotalWaitingCount(): Long {
        return queueRepository.getTotalWaitingCount()
    }

    fun processQueue(): List<Long> {
        val memberIds = queueRepository.popFromQueue(BATCH_SIZE)
        memberIds.forEach { memberId ->
            val token = UUID.randomUUID().toString()
            queueRepository.issueToken(memberId, token, TOKEN_TTL_SECONDS)
        }
        return memberIds
    }

    fun validateToken(memberId: Long, token: String) {
        val storedToken = queueRepository.getToken(memberId)
            ?: throw CoreException(ErrorType.FORBIDDEN, QueueErrorCode.TOKEN_EXPIRED)

        if (storedToken != token) {
            throw CoreException(ErrorType.FORBIDDEN, QueueErrorCode.INVALID_TOKEN)
        }
    }

    fun consumeToken(memberId: Long) {
        queueRepository.deleteToken(memberId)
    }

    private fun calculateEstimatedWaitSeconds(position: Long): Long {
        if (position <= 0) return 0
        return (position / THROUGHPUT_PER_SECOND).toLong()
    }
}
