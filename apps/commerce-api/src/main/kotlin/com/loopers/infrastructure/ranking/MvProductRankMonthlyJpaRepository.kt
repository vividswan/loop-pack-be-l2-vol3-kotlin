package com.loopers.infrastructure.ranking

import com.loopers.domain.productrank.MvProductRankMonthlyModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface MvProductRankMonthlyJpaRepository : JpaRepository<MvProductRankMonthlyModel, Long> {

    fun findByPeriodKeyOrderByRankPositionAsc(
        periodKey: String,
        pageable: Pageable,
    ): List<MvProductRankMonthlyModel>
}
