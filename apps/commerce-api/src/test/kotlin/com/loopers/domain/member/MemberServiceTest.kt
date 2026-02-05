package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class MemberServiceTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Spy
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    @InjectMocks
    private lateinit var memberService: MemberService

    @DisplayName("회원가입 시,")
    @Nested
    inner class Register {

        @DisplayName("유효한 정보로 가입하면, 회원이 저장된다.")
        @Test
        fun savesMember_whenValidInfoIsProvided() {
            // arrange
            val command = MemberCommand.Register(
                loginId = "testuser",
                password = "Test1234!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            whenever(memberRepository.existsByLoginId(command.loginId)).thenReturn(false)
            whenever(memberRepository.save(any())).thenAnswer { invocation ->
                invocation.getArgument<MemberModel>(0)
            }

            // act
            val result = memberService.register(command)

            // assert
            assertThat(result.loginId).isEqualTo(command.loginId)
            assertThat(result.name).isEqualTo(command.name)
            assertThat(result.email).isEqualTo(command.email)
        }

        @DisplayName("유효한 정보로 가입하면, Repository의 save가 호출된다.")
        @Test
        fun callsRepositorySave_whenValidInfoIsProvided() {
            // arrange
            val command = MemberCommand.Register(
                loginId = "testuser",
                password = "Test1234!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            whenever(memberRepository.existsByLoginId(command.loginId)).thenReturn(false)
            whenever(memberRepository.save(any())).thenAnswer { invocation ->
                invocation.getArgument<MemberModel>(0)
            }

            // act
            memberService.register(command)

            // assert
            verify(memberRepository).save(any())
        }

        @DisplayName("이미 존재하는 로그인 ID면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdAlreadyExists() {
            // arrange
            val command = MemberCommand.Register(
                loginId = "existinguser",
                password = "Test1234!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            whenever(memberRepository.existsByLoginId(command.loginId)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                memberService.register(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 암호화되어 저장된다.")
        @Test
        fun encryptsPassword_whenSavingMember() {
            // arrange
            val rawPassword = "Test1234!"
            val command = MemberCommand.Register(
                loginId = "testuser",
                password = rawPassword,
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            whenever(memberRepository.existsByLoginId(command.loginId)).thenReturn(false)

            val memberCaptor = argumentCaptor<MemberModel>()
            whenever(memberRepository.save(memberCaptor.capture())).thenAnswer { invocation ->
                invocation.getArgument<MemberModel>(0)
            }

            // act
            memberService.register(command)

            // assert
            val savedMember = memberCaptor.firstValue
            verify(passwordEncoder).encode(rawPassword)
            assertThat(savedMember.password).isNotEqualTo(rawPassword)
            assertThat(passwordEncoder.matches(rawPassword, savedMember.password)).isTrue()
        }
    }

    @DisplayName("인증 시,")
    @Nested
    inner class Authenticate {

        @DisplayName("유효한 정보로 인증하면, 회원 정보가 반환된다.")
        @Test
        fun returnsMember_whenValidCredentialsAreProvided() {
            // arrange
            val rawPassword = "Test1234!"
            val encodedPassword = passwordEncoder.encode(rawPassword)
            val member = MemberModel(
                loginId = "testuser",
                password = encodedPassword,
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            val command = MemberCommand.Authenticate(
                loginId = "testuser",
                password = rawPassword,
            )

            whenever(memberRepository.findByLoginId(command.loginId)).thenReturn(member)

            // act
            val result = memberService.authenticate(command)

            // assert
            assertThat(result.loginId).isEqualTo(member.loginId)
            assertThat(result.name).isEqualTo(member.name)
        }

        @DisplayName("존재하지 않는 로그인 ID면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenLoginIdDoesNotExist() {
            // arrange
            val command = MemberCommand.Authenticate(
                loginId = "nonexistent",
                password = "Test1234!",
            )

            whenever(memberRepository.findByLoginId(command.loginId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                memberService.authenticate(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenPasswordDoesNotMatch() {
            // arrange
            val encodedPassword = passwordEncoder.encode("CorrectPass1!")
            val member = MemberModel(
                loginId = "testuser",
                password = encodedPassword,
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            val command = MemberCommand.Authenticate(
                loginId = "testuser",
                password = "WrongPass123!",
            )

            whenever(memberRepository.findByLoginId(command.loginId)).thenReturn(member)

            // act
            val exception = assertThrows<CoreException> {
                memberService.authenticate(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
