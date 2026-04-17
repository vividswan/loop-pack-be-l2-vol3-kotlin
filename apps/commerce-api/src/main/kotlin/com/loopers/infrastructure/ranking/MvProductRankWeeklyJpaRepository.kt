package com.loopers.infrastructure.ranking

import com.loopers.domain.productrank.MvProductRankWeeklyModel
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface MvProductRankWeeklyJpaRepository : JpaRepository<MvProductRankWeeklyModel, Long> {

    fun findByPeriodKeyOrderByRankPositionAsc(
        periodKey: String,
        pageable: Pageable,
    ): List<MvProductRankWeeklyModel>
}
