package com.loopers.interfaces.api.queue

import com.loopers.application.member.MemberInfo
import com.loopers.application.queue.QueueFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue")
class QueueV1Controller(
    private val queueFacade: QueueFacade,
) : QueueV1ApiSpec {

    @PostMapping("/enter")
    @ResponseStatus(HttpStatus.CREATED)
    override fun enterQueue(
        @AuthenticatedMember memberInfo: MemberInfo,
    ): ApiResponse<QueueV1Dto.EnterResponse> {
        return queueFacade.enterQueue(memberInfo.id)
            .let { QueueV1Dto.EnterResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/position")
    override fun getPosition(
        @AuthenticatedMember memberInfo: MemberInfo,
    ): ApiResponse<QueueV1Dto.PositionResponse> {
        return queueFacade.getQueueStatus(memberInfo.id)
            .let { QueueV1Dto.PositionResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
