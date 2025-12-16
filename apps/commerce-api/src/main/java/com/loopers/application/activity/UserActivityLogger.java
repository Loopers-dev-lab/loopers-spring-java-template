package com.loopers.application.activity;

import com.loopers.domain.activity.event.UserActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 사용자 행동 로깅 전용 Listener
 *
 * UserActivityEvent를 수신하여 로그로 기록(비동기 처리)
 */
@Slf4j
@Component
public class UserActivityLogger {

    /**
     * 사용자 행동 로깅
     *
     * @Async로 비동기 처리하여 메인 비즈니스 로직에 영향 없음
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void logActivity(UserActivityEvent event) {
        try {
            log.info("USER_ACTIVITY userId={} action={} resourceType={} resourceId={} timestamp={}",
                    event.userId(),
                    event.action(),
                    event.resourceType(),
                    event.resourceId(),
                    event.timestamp()
            );

        } catch (Exception e) {
            // 로깅 실패해도 비즈니스 로직에 영향 없도록
            log.error("사용자 행동 로깅 실패 - userId: {}, action: {}",
                     event.userId(), event.action(), e);
        }
    }
}
