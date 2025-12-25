package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.event.CardPaymentProcessingStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Payment Domain Event Listener
 * 처리하는 Event:
 * - CardPaymentProcessingStartedEvent: 비동기 PG 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    @Value("${payment.callback.base-url}")
    private String callbackBaseUrl;

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;

    /**
     * PG 결제 처리 이벤트
     * Payment가 PENDING 상태로 저장된 후 발행되는 이벤트
     *
     * 1. Payment 조회
     * 2. PG 호출 (외부 API)
     * 3. 성공 시: Payment → PROCESSING, Order → RECEIVED
     * 4. 실패 시: Payment PENDING 유지 (Scheduler가 재시도)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCardPaymentProcessingStarted(CardPaymentProcessingStartedEvent event) {
        log.info("[PG 호출 시작] paymentId={}, orderId={}, startedAt={}",
                event.paymentId(), event.orderId(), event.startedAt());

        try {
            // 1. Payment와 Order 조회
            Payment payment = paymentService.getPaymentByPaymentId(event.paymentId());
            Order order = orderService.getOrderById(event.orderId());

            // 2. PG 호출 (외부 API - 시간이 걸릴 수 있음)
            String callbackUrl = callbackBaseUrl + "/api/v1/payments/callback";
            PaymentResult result = paymentGateway.processPayment(
                    event.userId(),
                    payment,
                    callbackUrl
            );

            // PG 응답 시간 측정
            Duration elapsed = Duration.between(event.startedAt(), LocalDateTime.now());

            // 3. PG 응답 처리
            if ("FAIL".equals(result.status())) {
                log.warn("[PG 즉시 실패] paymentId={}, orderId={}, 소요시간={}ms",
                        event.paymentId(), event.orderId(), elapsed.toMillis());
                return;
            }

            // 4. PG 성공 시 상태 업데이트 (JPA 변경 감지로 자동 저장)
            payment.startProcessing(result.transactionId());
            order.updateStatus(OrderStatus.RECEIVED);

            log.info("[PG 호출 성공] paymentId={}, orderId={}, transactionId={}, 소요시간={}ms",
                    event.paymentId(), event.orderId(), result.transactionId(), elapsed.toMillis());

            // 응답 시간 경고
            if (elapsed.toMillis() > 3000) {
                log.warn("[PG 응답 지연] paymentId={}, 소요시간={}ms (3초 초과)",
                        event.paymentId(), elapsed.toMillis());
            }

        } catch (Exception e) {
            Duration elapsed = Duration.between(event.startedAt(), LocalDateTime.now());
            log.error("[PG 호출 실패] paymentId={}, 소요시간={}ms, Payment는 PENDING 유지 (Scheduler 재시도 대상)",
                    event.paymentId(), elapsed.toMillis(), e);
            // Payment는 PENDING 유지 - Scheduler가 나중에 재시도
        }
    }
}
