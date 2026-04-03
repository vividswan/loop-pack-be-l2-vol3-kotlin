package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueInfo

class QueueV1Dto {
    data class EnterResponse(
        val status: String,
        val position: Long?,
        val totalWaiting: Long?,
    ) {
        companion object {
            fun from(info: QueueInfo): EnterResponse {
                return EnterResponse(
                    status = info.status,
                    position = info.position,
                    totalWaiting = info.totalWaiting,
                )
            }
        }
    }

    data class PositionResponse(
        val status: String,
        val position: Long?,
        val estimatedWaitSeconds: Long?,
        val totalWaiting: Long?,
        val token: String?,
    ) {
        companion object {
            fun from(info: QueueInfo): PositionResponse {
                return PositionResponse(
                    status = info.status,
                    position = info.position,
                    estimatedWaitSeconds = info.estimatedWaitSeconds,
                    totalWaiting = info.totalWaiting,
                    token = info.token,
                )
            }
        }
    }
}
