package com.loopers.domain.member

class MemberCommand {
    data class Register(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: String,
        val email: String,
    )

    data class Authenticate(
        val loginId: String,
        val password: String,
    )
}
