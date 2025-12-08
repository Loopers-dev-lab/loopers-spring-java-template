package com.loopers.application.order;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.event.PaymentTimeoutEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderPaymentEventListener {
    private final OrderFacade orderFacade;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            Long orderId = event.orderId();
            orderFacade.confirmOrderByPayment(orderId, event.userId());
            log.info("주문 확정 완료 - orderNumber: {}", orderId);
        } catch (Exception e) {
            log.error("주문 확정 실패 - orderNumber: {}", event.orderId(), e);
            // TODO: 실패 시 알림 또는 재처리 큐에 넣기
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        try {
            Long orderId = event.orderId();
            orderFacade.cancelOrderByPaymentFailure(orderId, event.userId());
            log.info("주문 취소 완료 - orderNumber: {}", orderId);
        } catch (Exception e) {
            log.error("주문 취소 실패 - orderNumber: {}", event.orderId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentTimeout(PaymentTimeoutEvent event) {
        try {
            orderFacade.cancelOrderByPaymentFailure(event.orderId(), event.userId());
            log.info("타임아웃으로 주문 취소 - orderNumber: {}", event.orderId());
        } catch (Exception e) {
            log.error("타임아웃 주문 취소 실패 - orderNumber: {}", event.orderId(), e);
        }
    }
}
