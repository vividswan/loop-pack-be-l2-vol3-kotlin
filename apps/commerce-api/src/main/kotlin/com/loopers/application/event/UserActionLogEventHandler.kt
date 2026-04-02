package com.loopers.application.event

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 유저 행동 로깅을 비동기로 처리한다.
 *
 * 조회, 클릭, 좋아요, 주문 등의 유저 행동을 이벤트로 수집하여
 * 서버 레벨 로깅으로 적재한다. 유실되어도 핵심 비즈니스에 영향 없음.
 */
@Component
class UserActionLogEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventTaskExecutor")
    fun handleUserAction(event: UserActionEvent) {
        log.info(
            "[유저 행동 로그] action={}, targetType={}, targetId={}, memberId={}, metadata={}",
            event.action,
            event.targetType,
            event.targetId,
            event.memberId,
            event.metadata,
        )
    }
}
