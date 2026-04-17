package com.loopers.batch.job.ranking

import com.loopers.batch.job.ranking.step.WeeklyRankingTasklet
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

@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingJobConfig.JOB_NAME)
@Configuration
class WeeklyRankingJobConfig(
    private val jobRepository: JobRepository,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val transactionManager: PlatformTransactionManager,
    private val weeklyRankingTasklet: WeeklyRankingTasklet,
) {
    @Bean(JOB_NAME)
    fun weeklyRankingJob(): Job {
        return JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(aggregateWeeklyRankStep())
            .listener(jobListener)
            .build()
    }

    @JobScope
    @Bean(STEP_AGGREGATE)
    fun aggregateWeeklyRankStep(): Step {
        return StepBuilder(STEP_AGGREGATE, jobRepository)
            .tasklet(weeklyRankingTasklet, transactionManager)
            .listener(stepMonitorListener)
            .build()
    }

    companion object {
        const val JOB_NAME = "weeklyRankingJob"
        private const val STEP_AGGREGATE = "aggregateWeeklyRank"
    }
}
