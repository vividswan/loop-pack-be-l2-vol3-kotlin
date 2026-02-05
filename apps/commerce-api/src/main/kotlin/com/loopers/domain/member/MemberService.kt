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
            throw CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 로그인 ID입니다.")
        }

        MemberModel.validateRawPassword(command.password, command.birthDate)

        val member = MemberModel(
            loginId = command.loginId,
            password = passwordEncoder.encode(command.password),
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
        )

        return memberRepository.save(member)
    }

    fun authenticate(command: MemberCommand.Authenticate): MemberModel {
        val member = memberRepository.findByLoginId(command.loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "존재하지 않는 회원입니다.")

        if (!passwordEncoder.matches(command.password, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")
        }

        return member
    }

    fun changePassword(command: MemberCommand.ChangePassword) {
        val member = memberRepository.findById(command.memberId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다.")

        if (!passwordEncoder.matches(command.currentPassword, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")
        }

        if (command.currentPassword == command.newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.")
        }

        MemberModel.validateRawPassword(command.newPassword, member.birthDate)

        member.changePassword(passwordEncoder.encode(command.newPassword))
    }
}
