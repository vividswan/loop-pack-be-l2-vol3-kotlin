package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingFacade
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rankings")
class RankingV1Controller(
    private val rankingFacade: RankingFacade,
) : RankingV1ApiSpec {

    @GetMapping
    override fun getTopRankings(
        @RequestParam(defaultValue = "DAILY") period: RankingPeriod,
        @RequestParam(defaultValue = "10") limit: Int,
    ): ApiResponse<RankingV1Dto.TopRankingsResponse> {
        return rankingFacade.getTopRankings(period, limit.toLong())
            .let { RankingV1Dto.TopRankingsResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/products/{productId}")
    override fun getProductRank(
        @PathVariable productId: Long,
    ): ApiResponse<RankingV1Dto.ProductRankResponse> {
        val rankInfo = rankingFacade.getProductRank(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND)
        return RankingV1Dto.ProductRankResponse.from(rankInfo)
            .let { ApiResponse.success(it) }
    }
}
