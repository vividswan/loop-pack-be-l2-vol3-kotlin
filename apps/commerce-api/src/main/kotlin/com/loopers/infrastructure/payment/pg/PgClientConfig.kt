package com.loopers.infrastructure.payment.pg

import feign.Request
import org.springframework.context.annotation.Bean
import java.util.concurrent.TimeUnit

class PgClientConfig {

    @Bean
    fun pgFeignOptions(): Request.Options {
        // connectTimeout: 1s, readTimeout: 5s
        return Request.Options(
            1,
            TimeUnit.SECONDS,
            5,
            TimeUnit.SECONDS,
            true,
        )
    }
}
