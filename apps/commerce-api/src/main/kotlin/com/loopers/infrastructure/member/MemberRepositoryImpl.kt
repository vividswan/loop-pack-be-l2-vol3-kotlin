package com.loopers.infrastructure.member

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.MemberRepository
import org.springframework.stereotype.Component

@Component
class MemberRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
) : MemberRepository {

    override fun save(member: MemberModel): MemberModel {
        return memberJpaRepository.save(member)
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return memberJpaRepository.existsByLoginId(loginId)
    }

    override fun findByLoginId(loginId: String): MemberModel? {
        return memberJpaRepository.findByLoginId(loginId)
    }

    override fun findById(id: Long): MemberModel? {
        return memberJpaRepository.findById(id).orElse(null)
    }
}
