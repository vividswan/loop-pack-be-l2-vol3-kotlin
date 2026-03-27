package com.loopers.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@EnableAsync
@Configuration
class AsyncConfig {

    @Bean("eventTaskExecutor")
    fun eventTaskExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 5
            maxPoolSize = 10
            queueCapacity = 50
            setThreadNamePrefix("event-handler-")
            initialize()
        }
    }

    /**
     * Outbox Kafka 발행 전용 스레드풀.
     *
     * Kafka 브로커 장애 시 send()가 타임아웃까지 블록되므로,
     * 도메인 이벤트 처리(eventTaskExecutor)와 격리하여
     * 좋아요 집계 등 핵심 비동기 로직이 지연되지 않도록 한다.
     */
    @Bean("outboxTaskExecutor")
    fun outboxTaskExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 3
            maxPoolSize = 5
            queueCapacity = 100
            setThreadNamePrefix("outbox-publisher-")
            initialize()
        }
    }
}
