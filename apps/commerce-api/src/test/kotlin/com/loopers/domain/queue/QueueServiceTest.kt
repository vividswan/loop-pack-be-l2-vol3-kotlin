package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class QueueServiceTest {

    @Mock
    private lateinit var queueRepository: QueueRepository

    @InjectMocks
    private lateinit var queueService: QueueService

    @Nested
    @DisplayName("enterQueue")
    inner class EnterQueue {

        @DisplayName("대기열에 진입하면 순번을 반환한다")
        @Test
        fun returnsPosition_whenEnterQueue() {
            // arrange
            val memberId = 1L
            whenever(queueRepository.addToQueue(memberId)).thenReturn(true)
            whenever(queueRepository.getPosition(memberId)).thenReturn(5L)

            // act
            val position = queueService.enterQueue(memberId)

            // assert
            assertThat(position).isEqualTo(5L)
        }

        @DisplayName("이미 대기열에 있는 유저가 다시 진입하면 예외가 발생한다")
        @Test
        fun throwsException_whenAlreadyInQueue() {
            // arrange
            val memberId = 1L
            whenever(queueRepository.addToQueue(memberId)).thenReturn(false)

            // act & assert
            assertThatThrownBy { queueService.enterQueue(memberId) }
                .isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("getQueueStatus")
    inner class GetQueueStatus {

        @DisplayName("토큰이 발급된 유저는 READY 상태를 반환한다")
        @Test
        fun returnsReady_whenTokenExists() {
            // arrange
            val memberId = 1L
            whenever(queueRepository.getToken(memberId)).thenReturn("test-token")

            // act
            val status = queueService.getQueueStatus(memberId)

            // assert
            assertThat(status.status).isEqualTo(QueueStatus.Status.READY)
            assertThat(status.token).isEqualTo("test-token")
        }

        @DisplayName("대기열에 있는 유저는 WAITING 상태와 예상 대기 시간을 반환한다")
        @Test
        fun returnsWaiting_whenInQueue() {
            // arrange
            val memberId = 1L
            whenever(queueRepository.getToken(memberId)).thenReturn(null)
            whenever(queueRepository.getPosition(memberId)).thenReturn(350L)

            // act
            val status = queueService.getQueueStatus(memberId)

            // assert
            assertThat(status.status).isEqualTo(QueueStatus.Status.WAITING)
            assertThat(status.position).isEqualTo(350L)
            assertThat(status.estimatedWaitSeconds).isEqualTo(2L)
        }

        @DisplayName("대기열에 없는 유저는 NOT_IN_QUEUE 상태를 반환한다")
        @Test
        fun returnsNotInQueue_whenNotInQueue() {
            // arrange
            val memberId = 1L
            whenever(queueRepository.getToken(memberId)).thenReturn(null)
            whenever(queueRepository.getPosition(memberId)).thenReturn(null)

            // act
            val status = queueService.getQueueStatus(memberId)

            // assert
            assertThat(status.status).isEqualTo(QueueStatus.Status.NOT_IN_QUEUE)
        }
    }

    @Nested
    @DisplayName("processQueue")
    inner class ProcessQueue {

        @DisplayName("대기열에서 N명을 꺼내 토큰을 발급한다")
        @Test
        fun issuesTokens_whenProcessQueue() {
            // arrange
            val memberIds = listOf(1L, 2L, 3L)
            whenever(queueRepository.popFromQueue(QueueService.BATCH_SIZE)).thenReturn(memberIds)

            // act
            val result = queueService.processQueue()

            // assert
            assertThat(result).hasSize(3)
            memberIds.forEach { memberId ->
                verify(queueRepository).issueToken(eq(memberId), any(), eq(QueueService.TOKEN_TTL_SECONDS))
            }
        }

        @DisplayName("대기열이 비어있으면 빈 리스트를 반환한다")
        @Test
        fun returnsEmpty_whenQueueIsEmpty() {
            // arrange
            whenever(queueRepository.popFromQueue(QueueService.BATCH_SIZE)).thenReturn(emptyList())

            // act
            val result = queueService.processQueue()

            // assert
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("validateToken")
    inner class ValidateToken {

        @DisplayName("유효한 토큰이면 예외가 발생하지 않는다")
        @Test
        fun noException_whenTokenIsValid() {
            // arrange
            val memberId = 1L
            val token = "valid-token"
            whenever(queueRepository.getToken(memberId)).thenReturn(token)

            // act & assert (no exception)
            queueService.validateToken(memberId, token)
        }

        @DisplayName("토큰이 만료되면 예외가 발생한다")
        @Test
        fun throwsException_whenTokenExpired() {
            // arrange
            val memberId = 1L
            whenever(queueRepository.getToken(memberId)).thenReturn(null)

            // act & assert
            assertThatThrownBy { queueService.validateToken(memberId, "some-token") }
                .isInstanceOf(CoreException::class.java)
        }

        @DisplayName("토큰이 일치하지 않으면 예외가 발생한다")
        @Test
        fun throwsException_whenTokenMismatch() {
            // arrange
            val memberId = 1L
            whenever(queueRepository.getToken(memberId)).thenReturn("stored-token")

            // act & assert
            assertThatThrownBy { queueService.validateToken(memberId, "wrong-token") }
                .isInstanceOf(CoreException::class.java)
        }
    }
}
