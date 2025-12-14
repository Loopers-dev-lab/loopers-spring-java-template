package com.loopers.infrastructure.tracking;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.tracking.event.UserBehaviorEvent;
import com.loopers.infrastructure.tracking.client.AnalyticsClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 유저 행동 추적 이벤트 핸들러 (Infrastructure Layer)
 * <p>
 * 유저 행동 이벤트를 수신하여 분석 시스템으로 전송합니다.
 * 트랜잭션과 완전 분리되어 비즈니스 로직에 영향을 주지 않습니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserBehaviorEventHandler {

    private final AnalyticsClient analyticsClient;

    /**
     * 유저 행동 이벤트 처리
     * <p>
     * AFTER_COMMIT + @Async로 완전한 트랜잭션 분리
     * 분석 시스템 전송 실패가 비즈니스 로직에 영향 주지 않음
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserBehavior(UserBehaviorEvent event) {
        try {
            log.debug("유저 행동 분석 데이터 전송 시작 - eventType: {}, userId: {}, targetId: {}",
                    event.eventType(), event.userId(), event.targetId());

            boolean success = analyticsClient.sendBehaviorData(event);

            if (success) {
                log.debug("유저 행동 분석 데이터 전송 성공 - eventType: {}, userId: {}",
                        event.eventType(), event.userId());
            } else {
                log.warn("유저 행동 분석 데이터 전송 실패 - eventType: {}, userId: {}",
                        event.eventType(), event.userId());
                // TODO: 실패한 이벤트를 재처리 큐에 넣거나 로컬 저장소에 백업
            }

        } catch (Exception e) {
            // 분석 시스템 전송 실패해도 비즈니스 로직에는 영향 없음
            log.error("유저 행동 분석 데이터 전송 중 예외 발생 - eventType: {}, userId: {}",
                    event.eventType(), event.userId(), e);

            // TODO: 실패한 이벤트를 재처리 큐에 넣거나 로컬 저장소에 백업
        }
    }
}
