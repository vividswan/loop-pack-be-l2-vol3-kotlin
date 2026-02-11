package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberFacade
import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberV1Controller(
    private val memberFacade: MemberFacade,
) : MemberV1ApiSpec {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestBody request: MemberV1Dto.RegisterRequest,
    ): ApiResponse<MemberV1Dto.RegisterResponse> {
        return memberFacade.register(request.toCommand())
            .let { MemberV1Dto.RegisterResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getMyInfo(
        @AuthenticatedMember memberInfo: MemberInfo,
    ): ApiResponse<MemberV1Dto.MyInfoResponse> {
        return MemberV1Dto.MyInfoResponse.from(memberInfo)
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/password")
    override fun changePassword(
        @AuthenticatedMember memberInfo: MemberInfo,
        @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Unit> {
        memberFacade.changePassword(request.toCommand(memberInfo.id))
        return ApiResponse.success(Unit)
    }
}
