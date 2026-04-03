package com.loopers.application.queue

import com.loopers.domain.queue.QueueService
import org.springframework.stereotype.Component

@Component
class QueueFacade(
    private val queueService: QueueService,
) {
    fun enterQueue(memberId: Long): QueueInfo {
        val position = queueService.enterQueue(memberId)
        val totalWaiting = queueService.getTotalWaitingCount()
        return QueueInfo.fromEnter(position, totalWaiting)
    }

    fun getQueueStatus(memberId: Long): QueueInfo {
        val queueStatus = queueService.getQueueStatus(memberId)
        val totalWaiting = queueService.getTotalWaitingCount()
        return QueueInfo.from(queueStatus, totalWaiting)
    }
}
