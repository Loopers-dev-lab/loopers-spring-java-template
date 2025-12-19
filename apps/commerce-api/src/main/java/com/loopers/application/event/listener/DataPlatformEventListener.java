package com.loopers.application.event.listener;

import com.loopers.application.event.order.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문/결제 결과를 데이터 플랫폼으로 전송하는 비동기 리스너
 * - 외부 I/O 실패 시에도 본 트랜잭션에 영향 없음
 * - 일시적 장애 대응을 위한 재시도 메커니즘 포함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventListener {

    /**
     * 주문 완료 이벤트를 데이터 플랫폼으로 전송 (비동기 + 재시도)
     *
     * 재시도 전략:
     * - 최대 3회 재시도
     * - 초기 딜레이 1초, 지수 백오프 (1s → 2s → 4s)
     * - 외부 API 일시 장애 대응 (503, timeout 등)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "recoverOrderCompleted"
    )
    public void handleOrderCompleted(OrderCompletedEvent event) {
        // TODO: 실제 데이터 플랫폼 API 연동. 현재는 더미 호출로 대체.
        log.info("[DataPlatform] 주문 완료 전송 시도 - orderNo: {}, memberId: {}, amount: {}",
                event.orderNo(), event.memberId(), event.totalPrice());

        // 실제 구현 예시:
        // dataPlatformClient.sendOrderCompleted(event);

        log.debug("[DataPlatform] 주문 완료 전송 성공 - orderNo: {}", event.orderNo());
    }

    /**
     * 데이터 플랫폼 전송 최종 실패 시 복구 메서드
     * - 3회 재시도 후에도 실패한 경우 호출됨
     * - 데이터 유실 방지를 위해 DLQ 저장 필요
     */
    @Recover
    public void recoverOrderCompleted(Exception ex, OrderCompletedEvent event) {
        log.error("[DataPlatform] 주문 완료 전송 최종 실패 - orderNo: {}, error: {}",
                event.orderNo(), ex.getMessage(), ex);

        // TODO: Dead Letter Queue에 저장 (중요!)
        // deadLetterQueueService.save(event, ex);
        // → 나중에 배치 작업으로 재전송

        // TODO: 알림 전송 (심각한 외부 시스템 장애)
        // alertService.sendAlert("데이터 플랫폼 전송 실패", event, ex);
    }
}
