package com.loopers.infrastructure.dataplatform;

import java.time.ZonedDateTime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.event.OrderCancelledEvent;
import com.loopers.domain.order.event.OrderConfirmedEvent;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.event.PaymentTimeoutEvent;
import com.loopers.infrastructure.dataplatform.dto.OrderDataDto;
import com.loopers.infrastructure.dataplatform.dto.PaymentDataDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 플랫폼 이벤트 핸들러 (Infrastructure Layer)
 * <p>
 * 주문/결제 완료 후 데이터 플랫폼으로 데이터를 전송합니다.
 * 트랜잭션 분리를 통한 외부 시스템 연동
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataPlatformEventHandler {

    private final DataPlatformClient dataPlatformClient;

    /**
     * 주문 확정 데이터 플랫폼 전송 이벤트 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        try {
            log.debug("주문 확정 데이터 플랫폼 전송 시작 - orderId: {}", event.orderId());

            OrderDataDto orderData = new OrderDataDto(
                event.orderId(),
                event.orderNumber(),
                event.userId(),
                OrderStatus.CONFIRMED,
                event.originalTotalAmount(),
                event.discountAmount(),
                event.finalTotalAmount(),
                ZonedDateTime.now(),
                "ORDER_CONFIRMED"
            );

            boolean success = dataPlatformClient.sendOrderData(orderData);

            if (success) {
                log.info("주문 확정 데이터 플랫폼 전송 성공 - orderId: {}", event.orderId());
            } else {
                log.warn("주문 확정 데이터 플랫폼 전송 실패 - orderId: {}", event.orderId());
            }

        } catch (Exception e) {
            log.error("주문 확정 데이터 플랫폼 전송 중 예외 발생 - orderId: {}", event.orderId(), e);
        }
    }

    /**
     * 주문 취소 데이터 플랫폼 전송 이벤트 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        try {
            log.debug("주문 취소 데이터 플랫폼 전송 시작 - orderId: {}", event.orderId());

            OrderDataDto orderData = new OrderDataDto(
                event.orderId(),
                event.orderNumber(),
                event.userId(),
                OrderStatus.CANCELLED,
                event.originalTotalAmount(),
                event.discountAmount(),
                event.finalTotalAmount(),
                ZonedDateTime.now(),
                "ORDER_CANCELLED"
            );

            boolean success = dataPlatformClient.sendOrderData(orderData);

            if (success) {
                log.info("주문 취소 데이터 플랫폼 전송 성공 - orderId: {}", event.orderId());
            } else {
                log.warn("주문 취소 데이터 플랫폼 전송 실패 - orderId: {}", event.orderId());
            }

        } catch (Exception e) {
            log.error("주문 취소 데이터 플랫폼 전송 중 예외 발생 - orderId: {}", event.orderId(), e);
        }
    }

    /**
     * 결제 완료 데이터 플랫폼 전송 이벤트 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.debug("결제 완료 데이터 플랫폼 전송 시작 - transactionKey: {}", event.transactionKey());

            PaymentDataDto paymentData = new PaymentDataDto(
                event.transactionKey(),
                event.orderId(),
                event.userId(),
                PaymentStatus.COMPLETED,
                event.amount(),
                event.cardType(),
                ZonedDateTime.now(),
                "PAYMENT_COMPLETED",
                null
            );

            boolean success = dataPlatformClient.sendPaymentData(paymentData);

            if (success) {
                log.info("결제 완료 데이터 플랫폼 전송 성공 - transactionKey: {}", event.transactionKey());
            } else {
                log.warn("결제 완료 데이터 플랫폼 전송 실패 - transactionKey: {}", event.transactionKey());
            }

        } catch (Exception e) {
            log.error("결제 완료 데이터 플랫폼 전송 중 예외 발생 - transactionKey: {}", event.transactionKey(), e);
        }
    }

    /**
     * 결제 실패 데이터 플랫폼 전송 이벤트 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        try {
            log.debug("결제 실패 데이터 플랫폼 전송 시작 - transactionKey: {}", event.transactionKey());

            PaymentDataDto paymentData = new PaymentDataDto(
                event.transactionKey(),
                event.orderId(),
                event.userId(),
                PaymentStatus.FAILED,
                event.amount(),
                event.cardType(),
                ZonedDateTime.now(),
                "PAYMENT_FAILED",
                event.reason()
            );

            boolean success = dataPlatformClient.sendPaymentData(paymentData);

            if (success) {
                log.info("결제 실패 데이터 플랫폼 전송 성공 - transactionKey: {}", event.transactionKey());
            } else {
                log.warn("결제 실패 데이터 플랫폼 전송 실패 - transactionKey: {}", event.transactionKey());
            }

        } catch (Exception e) {
            log.error("결제 실패 데이터 플랫폼 전송 중 예외 발생 - transactionKey: {}", event.transactionKey(), e);
        }
    }

    /**
     * 결제 타임아웃 데이터 플랫폼 전송 이벤트 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentTimeout(PaymentTimeoutEvent event) {
        try {
            log.debug("결제 타임아웃 데이터 플랫폼 전송 시작 - transactionKey: {}", event.transactionKey());

            PaymentDataDto paymentData = new PaymentDataDto(
                event.transactionKey(),
                event.orderId(),
                event.userId(),
                PaymentStatus.TIMEOUT,
                event.amount(),
                event.cardType(),
                ZonedDateTime.now(),
                "PAYMENT_TIMEOUT",
                "결제 콜백 타임아웃 (10분 초과)"
            );

            boolean success = dataPlatformClient.sendPaymentData(paymentData);

            if (success) {
                log.info("결제 타임아웃 데이터 플랫폼 전송 성공 - transactionKey: {}", event.transactionKey());
            } else {
                log.warn("결제 타임아웃 데이터 플랫폼 전송 실패 - transactionKey: {}", event.transactionKey());
            }

        } catch (Exception e) {
            log.error("결제 타임아웃 데이터 플랫폼 전송 중 예외 발생 - transactionKey: {}", event.transactionKey(), e);
        }
    }
}
