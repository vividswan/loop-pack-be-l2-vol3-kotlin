package com.loopers.domain.member

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Entity
@Table(name = "member")
class MemberModel internal constructor(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {

    @Column(name = "login_id", nullable = false, unique = true)
    var loginId: String = loginId
        protected set

    @Column(name = "password", nullable = false)
    var password: String = password
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate = birthDate
        protected set

    @Column(name = "email", nullable = false)
    var email: String = email
        protected set

    init {
        validateLoginId(loginId)
        validateName(name)
        validateEmail(email)
        validateBirthDate(birthDate)
    }

    fun changePassword(
        rawPassword: String,
        passwordEncoder: PasswordEncoder,
    ) {
        validateRawPassword(rawPassword, this.birthDate)
        this.password = passwordEncoder.encode(rawPassword)
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val PASSWORD_REGEX = Regex("^[A-Za-z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$")
        private val LOGIN_ID_REGEX = Regex("^[A-Za-z0-9]+$")
        private val BIRTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

        fun create(
            loginId: String,
            rawPassword: String,
            passwordEncoder: PasswordEncoder,
            name: String,
            birthDate: LocalDate,
            email: String,
        ): MemberModel {
            validateRawPassword(rawPassword, birthDate)
            return MemberModel(
                loginId = loginId,
                password = passwordEncoder.encode(rawPassword),
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }

        fun parseBirthDate(birthDateString: String): LocalDate {
            if (birthDateString.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.")
            }
            try {
                val date = LocalDate.parse(birthDateString, BIRTH_DATE_FORMATTER)
                if (date.isAfter(LocalDate.now())) {
                    throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 미래일 수 없습니다.")
                }
                return date
            } catch (e: DateTimeParseException) {
                throw CoreException(ErrorType.BAD_REQUEST, "생년월일 형식이 올바르지 않습니다. (YYYYMMDD)")
            }
        }

        fun formatBirthDate(birthDate: LocalDate): String {
            return birthDate.format(BIRTH_DATE_FORMATTER)
        }

        private fun validateRawPassword(password: String, birthDate: LocalDate) {
            if (password.length < 8) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다.")
            }
            if (password.length > 16) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 16자 이하여야 합니다.")
            }
            if (!PASSWORD_REGEX.matches(password)) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 가능합니다.")
            }
            val birthDateString = formatBirthDate(birthDate)
            if (password.contains(birthDateString)) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
            }
        }

        private fun validateLoginId(loginId: String) {
            if (loginId.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")
            }
            if (!LOGIN_ID_REGEX.matches(loginId)) {
                throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자만 허용됩니다.")
            }
        }

        private fun validateName(name: String) {
            if (name.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
            }
        }

        private fun validateEmail(email: String) {
            if (email.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
            }
            if (!EMAIL_REGEX.matches(email)) {
                throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
            }
        }

        private fun validateBirthDate(birthDate: LocalDate) {
            if (birthDate.isAfter(LocalDate.now())) {
                throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 미래일 수 없습니다.")
            }
        }
    }
}
