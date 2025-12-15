package com.loopers.application.order.event;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.application.payment.event.PaymentSucceededEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final PaymentFacade paymentFacade;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 생성 트랜잭션 커밋 이후 결제를 진행합니다.
     * @param event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void handleOrderCreatedPayment(OrderCreatedEvent event) {
        try {
            paymentFacade.pay(event.orderId());
            eventPublisher.publishEvent(PaymentSucceededEvent.from(event.orderId()));
        } catch (Exception e) {
            // 사용자 요청 이상인 경우만 실패 처리
            // 이외 서버 타임아웃 등은 retry -> pending상태로 스케줄링 시도
            if (e instanceof CoreException ce && ce.getErrorType() == ErrorType.BAD_REQUEST) {
                eventPublisher.publishEvent(PaymentFailedEvent.of(event.userId(), event.orderId(), e));
            }
        }
    }
}
