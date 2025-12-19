package com.loopers.application.event.listener;

import com.loopers.application.event.tracking.UserActionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 유저 행동 이벤트를 서버 레벨에서 로깅/추적한다.
 * 실패해도 본 트랜잭션에는 영향을 주지 않는다.
 */
@Slf4j
@Component
public class UserActionLoggingListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void logUserAction(UserActionEvent event) {
        try {
            log.info("[UserAction] action={}, memberId={}, resourceType={}, resourceId={}, metadata={}, at={}",
                    event.action(), event.memberId(), event.resourceType(), event.resourceId(), event.metadata(), event.occurredAt());
        } catch (Exception e) {
            log.error("[UserAction] 로그 기록 실패 - action={}, memberId={}", event.action(), event.memberId(), e);
        }
    }
}
