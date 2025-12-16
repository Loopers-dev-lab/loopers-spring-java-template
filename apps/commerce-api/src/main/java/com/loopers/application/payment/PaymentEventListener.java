package com.loopers.application.payment;

import com.loopers.application.order.OrderCompensationService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.event.CardPaymentProcessingStartedEvent;
import com.loopers.domain.payment.event.CardPaymentRequestedEvent;
import com.loopers.domain.payment.event.PointPaymentRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    @Value("${payment.callback.base-url}")
    private String callbackBaseUrl;

    private final PaymentProcessor paymentProcessor;
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;
    private final OrderCompensationService compensationService;

    /**
     * 포인트 결제 이벤트 처리
     * - 트랜잭션 커밋 후 비동기 실행
     * - 이벤트 리스너 자체는 독립 트랜잭션 (REQUIRES_NEW)
     * - PaymentProcessor는 해당 트랜잭션 내에서 실행 (REQUIRED)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePointPaymentRequest(PointPaymentRequestedEvent event) {
        try {
            // 이미 처리된 주문인지 확인 (멱등성)
            if (checkedOrderStatusComplete(event.getOrderId())) {
                return;
            }

            // 포인트 결제 처리
            paymentProcessor.processPointPayment(
                    event.getUserId(),
                    event.getOrderId()
            );
        } catch (Exception e) {
            log.error("포인트 결제 실패 : {}", event.getOrderId(), e);

            // 보상 트랜잭션 실행 (포인트, 재고, 쿠폰 복구)
            Order order = orderService.getOrderById(event.getOrderId());
            compensationService.compensateOrderWithPointRefund(order.getId());

            throw e;
        }
    }

    /**
     * 카드 결제 이벤트 처리
     * - 트랜잭션 커밋 후 비동기 실행
     * - 이벤트 리스너 자체는 독립 트랜잭션 (REQUIRES_NEW)
     * - PaymentProcessor는 해당 트랜잭션 내에서 실행 (REQUIRED)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCardPaymentRequest(CardPaymentRequestedEvent event) {
        try {
            paymentProcessor.processCardPayment(
                    event.getOrderId(),
                    event.getCardType(),
                    event.getCardNo()
            );
        } catch (Exception e) {
            log.error("카드 결제 저장 실패: {}", event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * PG 결제 처리 (Payment 저장 후 발행되는 이벤트)
     * - DB 커넥션을 점유하지 않음
     * - PG 호출 실패해도 Payment는 PENDING으로 유지
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCardPaymentProcessingStarted(CardPaymentProcessingStartedEvent event) {
        log.info("PG 결제 요청 시작 - PaymentId: {}, OrderId: {}",
                event.getPaymentId(), event.getOrderId());

        try {
            // Payment 조회
            Payment payment = paymentService.getPaymentByPaymentId(event.getPaymentId());
            Order order = orderService.getOrderById(event.getOrderId());

            // PG 호출 (이미 DB 커넥션 없음)
            String callbackUrl = callbackBaseUrl + "/api/v1/payments/callback";
            PaymentResult result = paymentGateway.processPayment(
                    event.getUserId(),
                    payment,
                    callbackUrl
            );

            if ("FAIL".equals(result.status())) {
                log.warn("PG에서 즉시 실패 응답. orderId={}, paymentId={}",
                        event.getOrderId(), event.getPaymentId());
                return;
            }

            // PG 성공 시 Payment 상태 업데이트
            payment.startProcessing(result.transactionId());
            paymentService.save(payment);
            order.updateStatus(OrderStatus.RECEIVED);

            log.info("PG 결제 요청 성공. orderId={}, paymentId={}, transactionId={}",
                    event.getOrderId(), event.getPaymentId(), result.transactionId());

        } catch (Exception e) {
            log.error("PG 호출 실패. Payment는 PENDING으로 유지. paymentId={}",
                    event.getPaymentId(), e);
            // Payment는 PENDING 유지 - Scheduler가 나중에 재시도
        }
    }

    /**
     * 주문 완료 상태 확인 (멱등성 보장)
     */
    private boolean checkedOrderStatusComplete(Long orderId) {
        Order order = orderService.getOrderById(orderId);

        // 이미 처리된 주문인지 확인
        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("이미 처리된 주문입니다. orderId={}", orderId);
            return true;
        }
        return false;
    }
}
