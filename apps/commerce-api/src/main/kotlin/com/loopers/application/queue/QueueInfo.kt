package com.loopers.application.queue

import com.loopers.domain.queue.QueueStatus

data class QueueInfo(
    val status: String,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val totalWaiting: Long?,
    val token: String?,
) {
    companion object {
        fun fromEnter(position: Long, totalWaiting: Long): QueueInfo {
            return QueueInfo(
                status = QueueStatus.Status.WAITING.name,
                position = position,
                estimatedWaitSeconds = null,
                totalWaiting = totalWaiting,
                token = null,
            )
        }

        fun from(queueStatus: QueueStatus, totalWaiting: Long): QueueInfo {
            return QueueInfo(
                status = queueStatus.status.name,
                position = queueStatus.position,
                estimatedWaitSeconds = queueStatus.estimatedWaitSeconds,
                totalWaiting = totalWaiting,
                token = queueStatus.token,
            )
        }
    }
}
