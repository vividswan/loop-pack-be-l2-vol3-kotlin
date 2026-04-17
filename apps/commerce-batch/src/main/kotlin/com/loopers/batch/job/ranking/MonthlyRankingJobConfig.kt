package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.MonthlyRankingTasklet
import com.loopers.batch.listener.JobListener
import com.loopers.batch.listener.StepMonitorListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = MonthlyRankingJobConfig.JOB_NAME)
@Configuration
class MonthlyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val transactionManager: PlatformTransactionManager,
    private val monthlyRankingTasklet: MonthlyRankingTasklet,
) {
    @Bean(JOB_NAME)
    fun monthlyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(aggregateMonthlyRankStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_AGGREGATE)
    fun aggregateMonthlyRankStep(): Step {
        return StepBuilder(STEP_AGGREGATE, jobRepository)
            .tasklet(monthlyRankingTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }

    companion object {
        const val JOB_NAME = "monthlyRankingJob"
        private const val STEP_AGGREGATE = "aggregateMonthlyRank"
    }
}
