package com.loopers.application.payment;

import com.loopers.application.order.OrderCompensationService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.order.event.OrderFailureEvent;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final OrderCompensationService compensationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * PG 콜백 처리
     */
    @Transactional
    public void handlePaymentCallback(PaymentCallbackInfo command) {
        log.info("Processing payment callback - TransactionKey: {}, Status: {}",
                command.transactionKey(), command.status());

        // 1. transactionKey로 Payment 조회
        Payment payment = paymentService.getPaymentByTransactionKey(command.transactionKey());

        // 2. 결제 상태별 처리
        if ("SUCCESS".equals(command.status())) {
            handleSuccessPayment(payment);
        } else if ("FAILED".equals(command.status())) {
            handleFailedPayment(payment, command.reason());
        } else {
            log.warn("Unknown payment status: {}", command.status());
        }
    }

    /**
     * 결제 성공 처리
     */
    private void handleSuccessPayment(Payment payment) {
        log.info("결제 성공 처리 시작 - PaymentId: {}", payment.getPaymentId());

        // 결제 완료 처리 (도메인 로직)
        payment.completePayment();

        // Order 완료 처리 (도메인 로직)
        Order order = payment.getOrder();
        order.completeOrder();

        eventPublisher.publishEvent(
                OrderCompletedEvent.of(
                        order.getId(),
                        order.getUser().getId(),
                        order.getTotalPrice().getAmount().toString(),
                        payment.getPaymentId(),
                        payment.getPaymentType().name()
                )
        );

        log.info("결제 성공 처리 완료 - PaymentId: {}, OrderId: {}",
                payment.getPaymentId(), order.getId());
    }

    /**
     * 결제 실패 처리
     * PG 결제 실패 시 호출됨
     */
    private void handleFailedPayment(Payment payment, String reason) {
        log.info("결제 실패 처리 시작 - PaymentId: {}, Reason: {}",
                payment.getPaymentId(), reason);

        // 멱등성 보장: 이미 실패 처리된 결제는 스킵
        if (payment.getStatus() == com.loopers.domain.payment.PaymentStatus.FAILED) {
            log.warn("이미 실패 처리된 결제입니다 - PaymentId: {}", payment.getPaymentId());
            return;
        }

        // 결제 실패 처리 (도메인 로직)
        payment.failPayment(reason);

        // Order 가져오기
        Order order = payment.getOrder();

        // 보상 트랜잭션 실행 (포인트 환불 포함)
        compensationService.compensateOrderWithPointRefund(order.getId());

        eventPublisher.publishEvent(
                OrderFailureEvent.of(
                        order.getId(),
                        order.getUser().getId(),
                        order.getTotalPrice().getAmount().toString(),
                        payment.getPaymentId(),
                        payment.getPaymentType().name()
                )
        );

        log.info("결제 실패 처리 완료 - PaymentId: {}, OrderId: {}",
                payment.getPaymentId(), order.getId());
    }
}
