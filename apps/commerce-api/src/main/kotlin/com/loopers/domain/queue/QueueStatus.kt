package com.loopers.domain.queue

data class QueueStatus(
    val status: Status,
    val position: Long?,
    val estimatedWaitSeconds: Long?,
    val token: String?,
) {
    enum class Status {
        WAITING,
        READY,
        NOT_IN_QUEUE,
    }

    companion object {
        fun waiting(position: Long, estimatedWaitSeconds: Long): QueueStatus {
            return QueueStatus(
                status = Status.WAITING,
                position = position,
                estimatedWaitSeconds = estimatedWaitSeconds,
                token = null,
            )
        }

        fun ready(token: String): QueueStatus {
            return QueueStatus(
                status = Status.READY,
                position = 0,
                estimatedWaitSeconds = 0,
                token = token,
            )
        }

        fun notInQueue(): QueueStatus {
            return QueueStatus(
                status = Status.NOT_IN_QUEUE,
                position = null,
                estimatedWaitSeconds = null,
                token = null,
            )
        }
    }
}
