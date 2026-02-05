package com.loopers.application.member

import com.loopers.domain.member.MemberModel

data class MemberInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val birthDate: String,
    val email: String,
) {
    fun getMaskedName(): String {
        if (name.isEmpty()) return name
        if (name.length == 1) return "*"
        return name.dropLast(1) + "*"
    }

    companion object {
        fun from(model: MemberModel): MemberInfo {
            return MemberInfo(
                id = model.id,
                loginId = model.loginId,
                name = model.name,
                birthDate = model.birthDate,
                email = model.email,
            )
        }
    }
}
