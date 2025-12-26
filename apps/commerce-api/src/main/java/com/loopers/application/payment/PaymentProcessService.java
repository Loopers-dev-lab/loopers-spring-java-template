package com.loopers.application.payment;

import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.application.payment.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessService {

    private final PaymentFacade paymentFacade;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void process(Long userId, Long orderId) {
        try {
            paymentFacade.pay(userId, orderId);
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생, orderId={}", orderId, e);
            throw e;
        }
    }

    @Transactional
    public void processPg(Long userId, Long orderId) {
        try {
            paymentFacade.payPg(orderId);
            eventPublisher.publishEvent(PaymentSucceededEvent.from(orderId));
        } catch (Exception e) {
            // 이외 서버 타임아웃 등은 retry -> pending상태로 스케줄링 시도
            log.error("외부 PG 결제 실패, 주문 ID: {}", orderId, e);
            eventPublisher.publishEvent(PaymentFailedEvent.of(userId, orderId, e));
        }
    }
}
