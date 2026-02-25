package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products/{productId}/likes")
class LikeV1Controller(
    private val likeFacade: LikeFacade,
) : LikeV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun like(
        @AuthenticatedMember memberInfo: MemberInfo,
        @PathVariable productId: Long,
    ): ApiResponse<LikeV1Dto.LikeResponse> {
        return likeFacade.like(memberInfo.id, productId)
            .let { LikeV1Dto.LikeResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping
    override fun unlike(
        @AuthenticatedMember memberInfo: MemberInfo,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.unlike(memberInfo.id, productId)
        return ApiResponse.success(Unit)
    }
}
