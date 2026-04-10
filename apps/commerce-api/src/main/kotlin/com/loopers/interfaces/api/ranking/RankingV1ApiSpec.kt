package com.loopers.interfaces.api.ranking

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Ranking", description = "상품 랭킹 API")
interface RankingV1ApiSpec {

    @Operation(summary = "Top-N 랭킹 조회", description = "일별 가중치 기반 인기 상품 랭킹을 조회합니다.")
    fun getTopRankings(limit: Int): ApiResponse<RankingV1Dto.TopRankingsResponse>

    @Operation(summary = "상품 순위 조회", description = "특정 상품의 현재 순위와 점수를 조회합니다.")
    fun getProductRank(productId: Long): ApiResponse<RankingV1Dto.ProductRankResponse>
}
