package com.loopers.interfaces.api.auth

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.core.MethodParameter
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.query.LdapQueryBuilder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticatedAdminArgumentResolver(
    private val ldapTemplate: LdapTemplate,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthenticatedAdmin::class.java) &&
            parameter.parameterType == AdminInfo::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AdminInfo {
        val adminId = webRequest.getHeader(HEADER_ADMIN_ID)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, customMessage = "관리자 ID 헤더가 누락되었습니다.")
        val adminPw = webRequest.getHeader(HEADER_ADMIN_PW)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, customMessage = "관리자 비밀번호 헤더가 누락되었습니다.")

        return authenticate(adminId, adminPw)
    }

    private fun authenticate(uid: String, password: String): AdminInfo {
        try {
            val query = LdapQueryBuilder.query()
                .where("uid").`is`(uid)

            val mapper = AttributesMapper<AdminInfo> { attributes ->
                val cn = attributes.get("cn")?.get()?.toString() ?: uid
                AdminInfo(uid = uid, name = cn)
            }

            val results: List<AdminInfo> = ldapTemplate.search(query, mapper)

            if (results.isEmpty()) {
                throw CoreException(ErrorType.UNAUTHORIZED, customMessage = "존재하지 않는 관리자입니다.")
            }

            ldapTemplate.contextSource.getContext(
                "cn=$uid,ou=admins,dc=loopers,dc=com",
                password,
            ).close()

            return results[0]
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            throw CoreException(ErrorType.UNAUTHORIZED, customMessage = "관리자 인증에 실패했습니다.")
        }
    }

    companion object {
        private const val HEADER_ADMIN_ID = "X-Loopers-Admin-Id"
        private const val HEADER_ADMIN_PW = "X-Loopers-Admin-Pw"
    }
}
