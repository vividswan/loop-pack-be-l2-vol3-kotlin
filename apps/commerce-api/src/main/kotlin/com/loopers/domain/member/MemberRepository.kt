package com.loopers.domain.member

interface MemberRepository {
    fun save(member: MemberModel): MemberModel
    fun existsByLoginId(loginId: String): Boolean
    fun findByLoginId(loginId: String): MemberModel?
}
