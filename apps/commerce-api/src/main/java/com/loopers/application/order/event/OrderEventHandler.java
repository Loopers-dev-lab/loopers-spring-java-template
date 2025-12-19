package com.loopers.application.order.event;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.order.event.PaymentCompletedEvent;
import com.loopers.domain.order.event.PaymentFailedEvent;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderDomainService orderDomainService;
    private final OrderFacade orderFacade;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retry(name = "orderEventRetry", fallbackMethod = "handlePaymentCompletedFallback")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신: orderId={}, pgTransactionId={}",
                event.getOrderId(), event.getPgTransactionId());

        Order order = orderDomainService.confirmOrder(event.getUserId(), event.getOrderId());
        eventPublisher.publishEvent(OrderCompletedEvent.from(order));
        log.info("주문 확정 처리 완료: orderId={}", event.getOrderId());
    }

    private void handlePaymentCompletedFallback(PaymentCompletedEvent event, Exception e) {
        log.error("주문 확정 처리 최종 실패: orderId={}, 수동 확인 필요", event.getOrderId(), e);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retry(name = "orderEventRetry", fallbackMethod = "handlePaymentFailedFallback")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("결제 실패 이벤트 수신: orderId={}, reason={}",
                event.getOrderId(), event.getFailureReason());

        orderFacade.handlePaymentFailure(event.getUserId(), event.getOrderId());
        log.info("주문 실패 처리 완료: orderId={}", event.getOrderId());
    }

    private void handlePaymentFailedFallback(PaymentFailedEvent event, Exception e) {
        log.error("주문 실패 처리 최종 실패: orderId={}, 수동 확인 필요", event.getOrderId(), e);
    }
}
