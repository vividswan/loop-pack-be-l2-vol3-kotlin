package com.loopers.config

import com.loopers.interfaces.api.auth.AuthenticatedAdminArgumentResolver
import com.loopers.interfaces.api.auth.AuthenticatedMemberArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authenticatedMemberArgumentResolver: AuthenticatedMemberArgumentResolver,
    private val authenticatedAdminArgumentResolver: AuthenticatedAdminArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedMemberArgumentResolver)
        resolvers.add(authenticatedAdminArgumentResolver)
    }
}
