package com.loopers.fixtures

import org.springframework.http.HttpHeaders
import java.time.LocalDate

object MemberTestFixture {

    const val DEFAULT_LOGIN_ID = "testuser"
    const val DEFAULT_PASSWORD = "Test1234!"
    const val DEFAULT_NAME = "홍길동"
    val DEFAULT_BIRTH_DATE: LocalDate = LocalDate.of(1990, 1, 1)
    const val DEFAULT_EMAIL = "test@example.com"

    const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
    const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"

    fun createAuthHeaders(
        loginId: String,
        password: String,
    ): HttpHeaders = HttpHeaders().apply {
        set(HEADER_LOGIN_ID, loginId)
        set(HEADER_LOGIN_PW, password)
    }
}
