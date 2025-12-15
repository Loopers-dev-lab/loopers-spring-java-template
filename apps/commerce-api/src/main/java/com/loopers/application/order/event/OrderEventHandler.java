package com.loopers.application.order.event;

import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.event.PaymentRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 이벤트 핸들러
 * - 주문 생성 후 후속 처리 담당
 * - 결제 요청 이벤트 발행
 *
 * Note: 쿠폰 사용은 OrderFacade에서 주문 생성 트랜잭션 내에서 처리됨 (동시성 제어)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 생성 이벤트 처리
     * - 트랜잭션 커밋 후 실행
     * - 결제 요청 이벤트 발행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 처리 - orderId: {}, eventId: {}",
            event.orderId(), event.eventId());

        try {
            // 결제 요청 이벤트 발행
            PaymentRequestedEvent paymentEvent = PaymentRequestedEvent.from(event);
            eventPublisher.publishEvent(paymentEvent);
            log.info("결제 요청 이벤트 발행 - orderId: {}", event.orderId());

        } catch (Exception e) {
            log.error("주문 생성 이벤트 처리 실패 - orderId: {}, error: {}",
                event.orderId(), e.getMessage(), e);
        }
    }
}
