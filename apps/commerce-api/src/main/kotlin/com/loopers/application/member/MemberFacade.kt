package com.loopers.application.member

import com.loopers.domain.member.MemberCommand
import com.loopers.domain.member.MemberService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberFacade(
    private val memberService: MemberService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun register(command: MemberCommand.Register): MemberInfo {
        val member = memberService.register(command)
        val memberInfo = MemberInfo.from(member)

        eventPublisher.publishEvent(
            MemberEvent.Registered(
                memberId = memberInfo.id,
                loginId = memberInfo.loginId,
                name = memberInfo.name,
                email = memberInfo.email,
            ),
        )

        return memberInfo
    }

    @Transactional(readOnly = true)
    fun authenticate(command: MemberCommand.Authenticate): MemberInfo {
        return memberService.authenticate(command)
            .let { MemberInfo.from(it) }
    }

    @Transactional
    fun changePassword(command: MemberCommand.ChangePassword) {
        memberService.changePassword(command)

        eventPublisher.publishEvent(
            MemberEvent.PasswordChanged(memberId = command.memberId),
        )
    }
}
