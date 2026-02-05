package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MemberModelTest {

    @DisplayName("회원을 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("모든 필드가 유효하면, 회원이 정상적으로 생성된다.")
        @Test
        fun createsMember_whenAllFieldsAreValid() {
            // arrange
            val loginId = "testuser"
            val password = "Test1234!"
            val name = "홍길동"
            val birthDate = "19900101"
            val email = "test@example.com"

            // act
            val member = MemberModel(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )

            // assert
            assertAll(
                { assertThat(member.loginId).isEqualTo(loginId) },
                { assertThat(member.password).isEqualTo(password) },
                { assertThat(member.name).isEqualTo(name) },
                { assertThat(member.birthDate).isEqualTo(birthDate) },
                { assertThat(member.email).isEqualTo(email) },
            )
        }
    }

    @DisplayName("로그인 ID 검증 시,")
    @Nested
    inner class LoginIdValidation {

        @DisplayName("빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        fun throwsBadRequest_whenLoginIdIsBlank(loginId: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = loginId,
                    password = "Test1234!",
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문과 숫자 외 문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["test_user", "test-user", "test@user", "test user", "테스트유저", "test!123"])
        fun throwsBadRequest_whenLoginIdContainsInvalidCharacters(loginId: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = loginId,
                    password = "Test1234!",
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문과 숫자만 포함되면, 정상적으로 생성된다.")
        @ParameterizedTest
        @ValueSource(strings = ["testuser", "TestUser123", "USER123", "abc123"])
        fun createsMember_whenLoginIdContainsOnlyAlphanumeric(loginId: String) {
            // act
            val member = MemberModel(
                loginId = loginId,
                password = "Test1234!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            // assert
            assertThat(member.loginId).isEqualTo(loginId)
        }
    }

    @DisplayName("이름 검증 시,")
    @Nested
    inner class NameValidation {

        @DisplayName("빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        fun throwsBadRequest_whenNameIsBlank(name: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser",
                    password = "Test1234!",
                    name = name,
                    birthDate = "19900101",
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("이메일 검증 시,")
    @Nested
    inner class EmailValidation {

        @DisplayName("빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        fun throwsBadRequest_whenEmailIsBlank(email: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser",
                    password = "Test1234!",
                    name = "홍길동",
                    birthDate = "19900101",
                    email = email,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["invalid", "invalid@", "@example.com", "invalid@.com", "invalid@example"])
        fun throwsBadRequest_whenEmailFormatIsInvalid(email: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser",
                    password = "Test1234!",
                    name = "홍길동",
                    birthDate = "19900101",
                    email = email,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("생년월일 검증 시,")
    @Nested
    inner class BirthDateValidation {

        @DisplayName("빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        fun throwsBadRequest_whenBirthDateIsBlank(birthDate: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser",
                    password = "Test1234!",
                    name = "홍길동",
                    birthDate = birthDate,
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("형식(YYYYMMDD)이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["1990-01-01", "90010101", "199001011", "1990/01/01", "19901301", "19900132"])
        fun throwsBadRequest_whenBirthDateFormatIsInvalid(birthDate: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser",
                    password = "Test1234!",
                    name = "홍길동",
                    birthDate = birthDate,
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("미래 날짜면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenBirthDateIsFuture() {
            // arrange
            val futureBirthDate = "29990101"

            // act
            val exception = assertThrows<CoreException> {
                MemberModel(
                    loginId = "testuser",
                    password = "Test1234!",
                    name = "홍길동",
                    birthDate = futureBirthDate,
                    email = "test@example.com",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("비밀번호 검증 시,")
    @Nested
    inner class PasswordValidation {

        @DisplayName("유효한 비밀번호면, 예외가 발생하지 않는다.")
        @Test
        fun doesNotThrow_whenPasswordIsValid() {
            // arrange
            val password = "Test1234!"
            val birthDate = "19900101"

            // act & assert
            MemberModel.validateRawPassword(password, birthDate)
        }

        @DisplayName("8자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["Test12!", "Abc123!", "Ab1!"])
        fun throwsBadRequest_whenPasswordIsTooShort(password: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel.validateRawPassword(password, "19900101")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("16자 초과면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordIsTooLong() {
            // arrange
            val password = "Test1234!Test1234!"

            // act
            val exception = assertThrows<CoreException> {
                MemberModel.validateRawPassword(password, "19900101")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문 대소문자, 숫자, 특수문자 외 문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["Test1234!한글", "Test 1234!", "Test1234!가"])
        fun throwsBadRequest_whenPasswordContainsInvalidCharacters(password: String) {
            // arrange & act
            val exception = assertThrows<CoreException> {
                MemberModel.validateRawPassword(password, "19900101")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["19900101!", "Test19900101", "a19900101b"])
        fun throwsBadRequest_whenPasswordContainsBirthDate(password: String) {
            // arrange
            val birthDate = "19900101"

            // act
            val exception = assertThrows<CoreException> {
                MemberModel.validateRawPassword(password, birthDate)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("비밀번호 변경 시,")
    @Nested
    inner class ChangePassword {

        @DisplayName("유효한 새 비밀번호로 변경하면, 비밀번호가 변경된다.")
        @Test
        fun changesPassword_whenNewPasswordIsValid() {
            // arrange
            val member = MemberModel(
                loginId = "testuser",
                password = "OldPass123!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )
            val newPassword = "NewPass456!"

            // act
            member.changePassword(newPassword)

            // assert
            assertThat(member.password).isEqualTo(newPassword)
        }

        @DisplayName("현재 비밀번호와 동일하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordIsSameAsCurrent() {
            // arrange
            val currentPassword = "Test1234!"
            val member = MemberModel(
                loginId = "testuser",
                password = currentPassword,
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            // act
            val exception = assertThrows<CoreException> {
                member.changePassword(currentPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
