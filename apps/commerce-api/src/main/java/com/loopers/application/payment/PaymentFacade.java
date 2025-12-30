package com.loopers.application.payment;

import com.loopers.domain.payment.event.PaymentEventPublisher;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.interfaces.api.payment.PaymentApiDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment Facade
 * 결제 관련 Application Layer
 */
@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private static final Logger log = LoggerFactory.getLogger(PaymentFacade.class);

    private final PaymentEventPublisher paymentEventPublisher;

    /**
     * PG 콜백 처리
     * PG Simulator로부터 결제 처리 결과를 받아 이벤트로 발행
     * 주문 관련 처리는 OrderEventListener가 이벤트를 통해 처리
     */
    @Transactional
    public PaymentInfo callbackPayment(PaymentApiDto.PgCallbackRequest request) {
        Long orderId = Long.parseLong(request.orderId());

        log.info("PG 콜백 수신 - orderId: {}, transactionKey: {}, status: {}", 
                orderId, request.transactionKey(), request.status());

        // PG 콜백 이벤트 발행 (PaymentEventListener가 처리)
        // PaymentEventListener가 paymentService를 호출하고, 그 다음 OrderEventListener가 주문 상태를 업데이트
        paymentEventPublisher.publishPaymentCallbackReceived(
            new PaymentEvents.CallbackReceived(
                request.transactionKey(),
                orderId,
                request.status(),
                request.reason()
            )
        );
        log.info("PG 콜백 이벤트 발행 완료 - orderId: {}", orderId);
        
        return PaymentInfo.builder()
                .orderId(orderId)
                .transactionKey(request.transactionKey())
                .status(request.status())
                .reason(request.reason())
                .build();
    }
}

