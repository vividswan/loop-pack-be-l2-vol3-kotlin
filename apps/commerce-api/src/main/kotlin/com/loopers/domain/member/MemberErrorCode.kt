package com.loopers.domain.member

import com.loopers.support.error.ErrorCode

enum class MemberErrorCode(override val code: String, override val message: String) : ErrorCode {
    // 로그인 ID 관련
    LOGIN_ID_EMPTY("member.login-id.empty", "로그인 ID는 비어있을 수 없습니다."),
    LOGIN_ID_INVALID_FORMAT("member.login-id.invalid-format", "로그인 ID는 영문과 숫자만 허용됩니다."),
    LOGIN_ID_DUPLICATE("member.login-id.duplicate", "이미 존재하는 로그인 ID입니다."),

    // 비밀번호 관련
    PASSWORD_TOO_SHORT("member.password.too-short", "비밀번호는 8자 이상이어야 합니다."),
    PASSWORD_TOO_LONG("member.password.too-long", "비밀번호는 16자 이하여야 합니다."),
    PASSWORD_INVALID_FORMAT("member.password.invalid-format", "비밀번호는 영문 대소문자, 숫자, 특수문자만 가능합니다."),
    PASSWORD_CONTAINS_BIRTH_DATE("member.password.contains-birth-date", "비밀번호에 생년월일을 포함할 수 없습니다."),
    PASSWORD_MISMATCH("member.password.mismatch", "비밀번호가 일치하지 않습니다."),
    PASSWORD_SAME_AS_CURRENT("member.password.same-as-current", "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다."),

    // 이름 관련
    NAME_EMPTY("member.name.empty", "이름은 비어있을 수 없습니다."),

    // 이메일 관련
    EMAIL_EMPTY("member.email.empty", "이메일은 비어있을 수 없습니다."),
    EMAIL_INVALID_FORMAT("member.email.invalid-format", "이메일 형식이 올바르지 않습니다."),

    // 생년월일 관련
    BIRTH_DATE_EMPTY("member.birth-date.empty", "생년월일은 비어있을 수 없습니다."),
    BIRTH_DATE_INVALID_FORMAT("member.birth-date.invalid-format", "생년월일 형식이 올바르지 않습니다. (YYYYMMDD)"),
    BIRTH_DATE_FUTURE("member.birth-date.future", "생년월일은 미래일 수 없습니다."),

    // 회원 관련
    MEMBER_NOT_FOUND("member.not-found", "존재하지 않는 회원입니다."),

    // 인증 헤더 관련
    AUTH_HEADER_LOGIN_ID_MISSING("member.auth.login-id-missing", "로그인 ID 헤더가 필요합니다."),
    AUTH_HEADER_PASSWORD_MISSING("member.auth.password-missing", "비밀번호 헤더가 필요합니다."),
}
