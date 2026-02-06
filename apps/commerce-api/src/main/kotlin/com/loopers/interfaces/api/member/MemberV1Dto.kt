package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberInfo
import com.loopers.domain.member.MemberCommand
import com.loopers.domain.member.MemberModel

class MemberV1Dto {
    data class RegisterRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: String,
        val email: String,
    ) {
        fun toCommand(): MemberCommand.Register {
            return MemberCommand.Register(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }
    }

    data class RegisterResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val birthDate: String,
        val email: String,
    ) {
        companion object {
            fun from(info: MemberInfo): RegisterResponse {
                return RegisterResponse(
                    id = info.id,
                    loginId = info.loginId,
                    name = info.name,
                    birthDate = MemberModel.formatBirthDate(info.birthDate),
                    email = info.email,
                )
            }
        }
    }

    data class MyInfoResponse(
        val loginId: String,
        val name: String,
        val birthDate: String,
        val email: String,
    ) {
        companion object {
            fun from(info: MemberInfo): MyInfoResponse {
                return MyInfoResponse(
                    loginId = info.loginId,
                    name = info.getMaskedName(),
                    birthDate = MemberModel.formatBirthDate(info.birthDate),
                    email = info.email,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
    ) {
        fun toCommand(memberId: Long): MemberCommand.ChangePassword {
            return MemberCommand.ChangePassword(
                memberId = memberId,
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
        }
    }
}
