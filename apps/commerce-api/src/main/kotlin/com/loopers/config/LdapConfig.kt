package com.loopers.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource

@Configuration
class LdapConfig(
    private val contextSource: LdapContextSource,
) {

    @Bean
    fun ldapTemplate(): LdapTemplate {
        return LdapTemplate(contextSource)
    }
}
