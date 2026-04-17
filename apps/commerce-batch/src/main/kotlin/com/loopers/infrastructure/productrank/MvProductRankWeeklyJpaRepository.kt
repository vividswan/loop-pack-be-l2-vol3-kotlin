package com.loopers.infrastructure.productrank

import com.loopers.domain.productrank.MvProductRankWeeklyModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MvProductRankWeeklyJpaRepository : JpaRepository<MvProductRankWeeklyModel, Long> {

    @Modifying
    @Query("DELETE FROM MvProductRankWeeklyModel m WHERE m.periodKey = :periodKey")
    fun deleteAllByPeriodKey(@Param("periodKey") periodKey: String)
}
