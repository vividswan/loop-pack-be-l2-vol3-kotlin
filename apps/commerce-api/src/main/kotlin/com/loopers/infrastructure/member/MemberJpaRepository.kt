package com.loopers.infrastructure.member

import com.loopers.domain.member.MemberModel
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<MemberModel, Long> {
    fun existsByLoginId(loginId: String): Boolean
    fun findByLoginId(loginId: String): MemberModel?
}
