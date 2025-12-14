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
public class OrderEventHandler {
    private final OrderFacade orderFacade;

    private void executeSafely(String action, Long orderId, Long userId, Runnable task) {
        if (orderId == null || userId == null) {
            log.warn("이벤트 무시 - 필수 값 누락 action={}, orderId={}, userId={}", action, orderId, userId);
            return;
        }
        try {
            log.debug("이벤트 처리 시작 action={}, orderId={}, userId={}", action, orderId, userId);
            task.run();
            log.info("이벤트 처리 성공 action={}, orderId={}, userId={}", action, orderId, userId);
        } catch (Exception e) {
            log.error("이벤트 처리 실패 action={}, orderId={}, userId={}", action, orderId, userId, e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        Long orderId = event.orderId();
        Long userId = event.userId();
        executeSafely("PAYMENT_COMPLETED", orderId, userId,
                () -> orderFacade.confirmOrderByPayment(orderId, userId));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Long orderId = event.orderId();
        Long userId = event.userId();
        executeSafely("PAYMENT_FAILED", orderId, userId,
                () -> orderFacade.cancelOrderByPaymentFailure(orderId, userId));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentTimeout(PaymentTimeoutEvent event) {
        Long orderId = event.orderId();
        Long userId = event.userId();
        executeSafely("PAYMENT_TIMEOUT", orderId, userId,
                () -> orderFacade.cancelOrderByPaymentFailure(orderId, userId));
    }
}
