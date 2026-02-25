package com.loopers.interfaces.api.like

import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Like", description = "좋아요 API")
interface LikeV1ApiSpec {

    @Operation(summary = "좋아요 등록", description = "상품에 좋아요를 등록합니다.")
    fun like(
        @AuthenticatedMember memberInfo: MemberInfo,
        productId: Long,
    ): ApiResponse<LikeV1Dto.LikeResponse>

    @Operation(summary = "좋아요 취소", description = "상품의 좋아요를 취소합니다.")
    fun unlike(
        @AuthenticatedMember memberInfo: MemberInfo,
        productId: Long,
    ): ApiResponse<Unit>
}
