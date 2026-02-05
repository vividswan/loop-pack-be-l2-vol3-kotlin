package com.loopers.application.member

import com.loopers.domain.member.MemberCommand
import com.loopers.domain.member.MemberService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberFacade(
    private val memberService: MemberService,
) {
    @Transactional
    fun register(command: MemberCommand.Register): MemberInfo {
        return memberService.register(command)
            .let { MemberInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun authenticate(command: MemberCommand.Authenticate): MemberInfo {
        return memberService.authenticate(command)
            .let { MemberInfo.from(it) }
    }

    @Transactional
    fun changePassword(command: MemberCommand.ChangePassword) {
        memberService.changePassword(command)
    }
}
