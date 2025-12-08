package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;

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

        log.info("결제 성공 처리 완료 - PaymentId: {}, OrderId: {}",
                payment.getPaymentId(), order.getId());
    }

    /**
     * 결제 실패 처리
     */
    private void handleFailedPayment(Payment payment, String reason) {
        log.info("결제 실패 처리 시작 payment - PaymentId: {}, Reason: {}",
                payment.getPaymentId(), reason);

        // 멱등성 보장: 이미 실패 처리된 결제는 스킵
        if (payment.getStatus() == com.loopers.domain.payment.PaymentStatus.FAILED) {
            log.warn("이미 실패 처리된 결제입니다 - PaymentId: {}", payment.getPaymentId());
            return;
        }

        // 결제 실패 처리 (도메인 로직)
        payment.failPayment(reason);

        // Order 취소 처리 (도메인 로직)
        Order order = payment.getOrder();

        // 보상 트랜잭션: 주문 상태 변경 전에 먼저 실행
        executeCompensationTransaction(order);

        // 보상 트랜잭션 완료 후 Order 상태 변경
        order.cancelOrder();

        log.info("결제 실패 처리 완료 - PaymentId: {}, OrderId: {}",
                payment.getPaymentId(), order.getId());
    }

    /**
     * 보상 트랜잭션 실행
     * 결제 실패 시 주문 생성 과정에서 차감된 재고, 포인트, 쿠폰을 복구
     */
    private void executeCompensationTransaction(Order order) {
        // 이미 취소된 주문이면 보상 트랜잭션 스킵 (멱등성 보장)
        if (order.getStatus() == com.loopers.domain.order.OrderStatus.CANCELED) {
            log.info("이미 보상 트랜잭션이 실행된 주문입니다 - OrderId: {}", order.getId());
            return;
        }

        log.info("보상 트랜잭션 시작 OrderId: {}", order.getId());

        // 1. 재고 복구
        order.getOrderItems().forEach(orderItem -> {
            orderItem.getProduct().increaseStock(orderItem.getQuantity());
            log.info("Stock restored - Product: {}, Quantity: {}",
                    orderItem.getProduct().getProductName(), orderItem.getQuantity());
        });

        // 2. 포인트 환불
        order.getUser().refundPoint(order.getTotalPrice());
        log.info("Point refunded - UserId: {}, Amount: {}",
                order.getUser().getUserId(), order.getTotalPrice().getAmount());

        // 3. 쿠폰 복구
        if (order.getIssuedCoupon() != null) {
            order.getIssuedCoupon().restoreCoupon();
            log.info("Coupon restored - IssuedCouponId: {}", order.getIssuedCoupon().getId());
        }

        log.info("보상 트랜잭션 완료 OrderId: {}", order.getId());
    }
}
