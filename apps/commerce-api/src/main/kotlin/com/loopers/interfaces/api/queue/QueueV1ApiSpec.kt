package com.loopers.interfaces.api.queue

import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Queue", description = "주문 대기열 API")
interface QueueV1ApiSpec {

    @Operation(summary = "대기열 진입", description = "주문 대기열에 진입합니다. 중복 진입은 불가합니다.")
    fun enterQueue(
        @AuthenticatedMember memberInfo: MemberInfo,
    ): ApiResponse<QueueV1Dto.EnterResponse>

    @Operation(summary = "대기열 순번 조회", description = "현재 대기 순번과 예상 대기 시간을 조회합니다. 토큰이 발급된 경우 토큰을 포함합니다.")
    fun getPosition(
        @AuthenticatedMember memberInfo: MemberInfo,
    ): ApiResponse<QueueV1Dto.PositionResponse>
}
