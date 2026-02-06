package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun register(command: MemberCommand.Register): MemberModel {
        if (memberRepository.existsByLoginId(command.loginId)) {
            throw CoreException(ErrorType.BAD_REQUEST, MemberErrorCode.LOGIN_ID_DUPLICATE.message)
        }

        val member = MemberModel.create(
            loginId = command.loginId,
            rawPassword = command.password,
            passwordEncoder = passwordEncoder,
            name = command.name,
            birthDate = MemberModel.parseBirthDate(command.birthDate),
            email = command.email,
        )

        return memberRepository.save(member)
    }

    fun authenticate(command: MemberCommand.Authenticate): MemberModel {
        val member = memberRepository.findByLoginId(command.loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, MemberErrorCode.MEMBER_NOT_FOUND.message)

        if (!passwordEncoder.matches(command.password, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, MemberErrorCode.PASSWORD_MISMATCH.message)
        }

        return member
    }

    fun changePassword(command: MemberCommand.ChangePassword) {
        val member = memberRepository.findById(command.memberId)
            ?: throw CoreException(ErrorType.NOT_FOUND, MemberErrorCode.MEMBER_NOT_FOUND.message)

        if (!passwordEncoder.matches(command.currentPassword, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, MemberErrorCode.PASSWORD_MISMATCH.message)
        }

        if (command.currentPassword == command.newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, MemberErrorCode.PASSWORD_SAME_AS_CURRENT.message)
        }

        member.changePassword(command.newPassword, passwordEncoder)
    }
}
