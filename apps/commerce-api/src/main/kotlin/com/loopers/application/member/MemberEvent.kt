package com.loopers.application.member

sealed class MemberEvent {
    data class Registered(
        val memberId: Long,
        val loginId: String,
        val name: String,
        val email: String,
    ) : MemberEvent()

    data class PasswordChanged(
        val memberId: Long,
    ) : MemberEvent()
}
