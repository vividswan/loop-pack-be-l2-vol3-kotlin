package com.loopers.interfaces.api.order

import com.loopers.application.member.MemberInfo
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Order", description = "주문 API")
interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    fun createOrder(
        @AuthenticatedMember memberInfo: MemberInfo,
        request: OrderV1Dto.CreateRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse>

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    fun getOrder(
        @AuthenticatedMember memberInfo: MemberInfo,
        id: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse>
}
