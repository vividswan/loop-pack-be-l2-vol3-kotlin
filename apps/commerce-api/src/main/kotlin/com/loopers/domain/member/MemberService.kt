package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
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

    @Transactional(readOnly = true)
    fun authenticate(command: MemberCommand.Authenticate): MemberModel {
        val member = memberRepository.findByLoginId(command.loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "존재하지 않는 회원입니다.")

        if (!passwordEncoder.matches(command.password, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")
        }

        return member
    }
}
