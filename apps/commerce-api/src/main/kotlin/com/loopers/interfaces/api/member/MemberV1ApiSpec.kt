package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "회원 API")
interface MemberV1ApiSpec {

    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    fun register(request: MemberV1Dto.RegisterRequest): ApiResponse<MemberV1Dto.RegisterResponse>

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 정보를 조회합니다.")
    fun getMyInfo(
        @AuthenticatedMember memberInfo: MemberInfo,
    ): ApiResponse<MemberV1Dto.MyInfoResponse>
}
