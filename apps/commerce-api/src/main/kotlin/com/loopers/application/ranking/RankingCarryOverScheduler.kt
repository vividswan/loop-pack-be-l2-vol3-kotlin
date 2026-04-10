package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 매일 자정에 전일 랭킹 점수의 일부를 오늘 키로 carry-over한다.
 *
 * 콜드 스타트 완화:
 * 새로운 일별 키가 생성되는 시점에는 점수가 0이므로 랭킹이 의미 없어진다.
 * 전일 점수의 10%를 복사하여 랭킹의 연속성을 유지한다.
 */
@Component
@ConditionalOnProperty(name = ["ranking.carry-over.enabled"], havingValue = "true", matchIfMissing = true)
class RankingCarryOverScheduler(
    private val rankingRepository: RankingRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 0 * * *")
    fun carryOver() {
        log.info("랭킹 carry-over 시작")
        rankingRepository.carryOverFromYesterday(CARRY_OVER_WEIGHT)
        log.info("랭킹 carry-over 완료")
    }

    companion object {
        private const val CARRY_OVER_WEIGHT = 0.1
    }
}
