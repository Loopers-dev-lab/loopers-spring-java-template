package com.loopers.infrastructure.dataplatform;

import com.loopers.domain.order.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

    private final DataPlatform dataPlatform;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            dataPlatform.send(event.getOrderId().toString());
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: orderId={}", event.getOrderId(), e);
        }
    }
}
