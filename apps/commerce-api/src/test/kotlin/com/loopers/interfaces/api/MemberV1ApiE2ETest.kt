package com.loopers.interfaces.api

import com.loopers.domain.member.MemberModel
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.member.MemberV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_REGISTER = "/api/v1/members/register"
        private const val ENDPOINT_MY_INFO = "/api/v1/members/me"
        private const val ENDPOINT_CHANGE_PASSWORD = "/api/v1/members/password"
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/members/register")
    @Nested
    inner class Register {

        @DisplayName("유효한 정보로 회원가입 요청하면, 201 Created 응답을 받는다.")
        @Test
        fun returnsCreated_whenValidRequestIsProvided() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = "Test1234!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.loginId).isEqualTo(request.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo(request.name) },
                { assertThat(response.body?.data?.email).isEqualTo(request.email) },
            )
        }

        @DisplayName("유효한 정보로 회원가입하면, 비밀번호가 암호화되어 저장된다.")
        @Test
        fun encryptsPassword_whenValidRequestIsProvided() {
            // arrange
            val rawPassword = "Test1234!"
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = rawPassword,
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            val savedMember = memberJpaRepository.findByLoginId(request.loginId)
            assertThat(savedMember).isNotNull
            assertThat(savedMember!!.password).isNotEqualTo(rawPassword)
            assertThat(passwordEncoder.matches(rawPassword, savedMember.password)).isTrue()
        }

        @DisplayName("이미 존재하는 로그인 ID로 가입 요청하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenLoginIdAlreadyExists() {
            // arrange
            val existingLoginId = "existinguser"
            memberJpaRepository.save(
                MemberModel(
                    loginId = existingLoginId,
                    password = passwordEncoder.encode("Test1234!"),
                    name = "기존회원",
                    birthDate = "19850101",
                    email = "existing@example.com",
                ),
            )

            val request = MemberV1Dto.RegisterRequest(
                loginId = existingLoginId,
                password = "NewPass123!",
                name = "신규회원",
                birthDate = "19900101",
                email = "new@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("잘못된 이메일 형식으로 요청하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenEmailFormatIsInvalid() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = "Test1234!",
                name = "홍길동",
                birthDate = "19900101",
                email = "invalid-email",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일이 포함되면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPasswordContainsBirthDate() {
            // arrange
            val birthDate = "19900101"
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = "Test$birthDate!",
                name = "홍길동",
                birthDate = birthDate,
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 8자 미만이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPasswordIsTooShort() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = "Test12!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/members/me")
    @Nested
    inner class GetMyInfo {

        @DisplayName("유효한 인증 정보로 요청하면, 200 OK와 마스킹된 이름을 반환한다.")
        @Test
        fun returnsOkWithMaskedName_whenValidCredentialsAreProvided() {
            // arrange
            val rawPassword = "Test1234!"
            val member = memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode(rawPassword),
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, member.loginId)
                set(HEADER_LOGIN_PW, rawPassword)
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_MY_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo(member.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") },
                { assertThat(response.body?.data?.birthDate).isEqualTo(member.birthDate) },
                { assertThat(response.body?.data?.email).isEqualTo(member.email) },
            )
        }

        @DisplayName("존재하지 않는 로그인 ID로 요청하면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLoginIdDoesNotExist() {
            // arrange
            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "nonexistent")
                set(HEADER_LOGIN_PW, "Test1234!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_MY_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("비밀번호가 일치하지 않으면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenPasswordDoesNotMatch() {
            // arrange
            memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode("CorrectPass1!"),
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, "WrongPass123!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_MY_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("한 글자 이름은 '*'로 마스킹된다.")
        @Test
        fun masksSingleCharacterName() {
            // arrange
            val rawPassword = "Test1234!"
            memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode(rawPassword),
                    name = "김",
                    birthDate = "19900101",
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, rawPassword)
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_MY_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.body?.data?.name).isEqualTo("*")
        }
    }

    @DisplayName("PUT /api/v1/members/password")
    @Nested
    inner class ChangePassword {

        @DisplayName("유효한 정보로 비밀번호 변경 요청하면, 200 OK 응답을 받는다.")
        @Test
        fun returnsOk_whenValidRequestIsProvided() {
            // arrange
            val oldPassword = "OldPass123!"
            val newPassword = "NewPass456!"
            memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode(oldPassword),
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, oldPassword)
            }

            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = oldPassword,
                newPassword = newPassword,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("비밀번호 변경 후, 새 비밀번호로 인증할 수 있다.")
        @Test
        fun canAuthenticateWithNewPassword_afterPasswordChange() {
            // arrange
            val oldPassword = "OldPass123!"
            val newPassword = "NewPass456!"
            memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode(oldPassword),
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                ),
            )

            val changeHeaders = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, oldPassword)
            }

            val changeRequest = MemberV1Dto.ChangePasswordRequest(
                currentPassword = oldPassword,
                newPassword = newPassword,
            )

            testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(changeRequest, changeHeaders),
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            // act
            val newHeaders = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, newPassword)
            }

            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_MY_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(newHeaders),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.loginId).isEqualTo("testuser")
        }

        @DisplayName("기존 비밀번호가 일치하지 않으면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenCurrentPasswordDoesNotMatch() {
            // arrange
            val correctPassword = "CorrectPass1!"
            memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode(correctPassword),
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, correctPassword)
            }

            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = "WrongPass123!",
                newPassword = "NewPass456!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("새 비밀번호가 현재 비밀번호와 동일하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNewPasswordIsSameAsCurrent() {
            // arrange
            val currentPassword = "Test1234!"
            memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode(currentPassword),
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, currentPassword)
            }

            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = currentPassword,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호에 생년월일이 포함되면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNewPasswordContainsBirthDate() {
            // arrange
            val currentPassword = "Test1234!"
            val birthDate = "19900101"
            memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode(currentPassword),
                    name = "홍길동",
                    birthDate = birthDate,
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, currentPassword)
            }

            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = "New$birthDate!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호가 8자 미만이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNewPasswordIsTooShort() {
            // arrange
            val currentPassword = "Test1234!"
            memberJpaRepository.save(
                MemberModel(
                    loginId = "testuser",
                    password = passwordEncoder.encode(currentPassword),
                    name = "홍길동",
                    birthDate = "19900101",
                    email = "test@example.com",
                ),
            )

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser")
                set(HEADER_LOGIN_PW, currentPassword)
            }

            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = "Short1!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
