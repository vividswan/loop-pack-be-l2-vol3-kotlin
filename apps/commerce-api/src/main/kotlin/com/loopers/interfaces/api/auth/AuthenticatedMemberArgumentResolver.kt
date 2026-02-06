package com.loopers.interfaces.api.auth

import com.loopers.application.member.MemberFacade
import com.loopers.application.member.MemberInfo
import com.loopers.domain.member.MemberCommand
import com.loopers.domain.member.MemberErrorCode
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticatedMemberArgumentResolver(
    private val memberFacade: MemberFacade,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthenticatedMember::class.java) &&
            parameter.parameterType == MemberInfo::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): MemberInfo {
        val loginId = webRequest.getHeader(HEADER_LOGIN_ID)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, MemberErrorCode.AUTH_HEADER_LOGIN_ID_MISSING)
        val password = webRequest.getHeader(HEADER_LOGIN_PW)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, MemberErrorCode.AUTH_HEADER_PASSWORD_MISSING)

        val command = MemberCommand.Authenticate(loginId = loginId, password = password)
        return memberFacade.authenticate(command)
    }

    companion object {
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }
}
