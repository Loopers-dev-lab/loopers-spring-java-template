package com.loopers.application.dataplatform;

import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.event.PaymentSuccessEvent;
import com.loopers.infrastructure.dataplatform.DataPlatformClient;
import com.loopers.infrastructure.dataplatform.DataPlatformOrderRequest;
import com.loopers.infrastructure.dataplatform.DataPlatformPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * 데이터 플랫폼 이벤트 핸들러
 * - 주문/결제 데이터를 외부 데이터 플랫폼으로 비동기 전송
 * - 핵심 비즈니스 로직과 완전히 분리되어 동작
 * - 실패해도 핵심 비즈니스에 영향을 주지 않음 (Eventual Consistency)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPlatformEventHandler {

    private final DataPlatformClient dataPlatformClient;

    /**
     * 주문 생성 이벤트 처리
     * - 주문 데이터를 데이터 플랫폼으로 전송
     * - 비동기로 동작하여 주문 생성 트랜잭션과 분리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("데이터 플랫폼 주문 데이터 전송 시작 - orderId: {}", event.orderId());

        try {
            DataPlatformOrderRequest request = new DataPlatformOrderRequest(
                event.orderId().toString(),
                event.userId(),
                "CREATED",
                event.totalAmount(),
                event.finalAmount(),
                event.userCouponId(),
                event.items().stream()
                    .map(item -> new DataPlatformOrderRequest.OrderItemData(
                        item.productId().toString(),
                        item.quantity(),
                        item.price()
                    ))
                    .collect(Collectors.toList()),
                event.createdAt().atZone(ZoneOffset.UTC)
            );

            dataPlatformClient.sendOrderData(event.userId(), request);

            log.info("데이터 플랫폼 주문 데이터 전송 완료 - orderId: {}", event.orderId());
        } catch (Exception e) {
            // 데이터 플랫폼 전송 실패는 핵심 비즈니스에 영향을 주지 않음
            log.error("데이터 플랫폼 주문 데이터 전송 실패 - orderId: {}, error: {}",
                event.orderId(), e.getMessage());
            // TODO: 재시도 큐에 등록하거나 관리자 알림
        }
    }

    /**
     * 결제 성공 이벤트 처리
     * - 결제 데이터를 데이터 플랫폼으로 전송
     * - 비동기로 동작하여 결제 트랜잭션과 분리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("데이터 플랫폼 결제 데이터 전송 시작 - paymentId: {}, orderId: {}",
            event.paymentId(), event.orderId());

        try {
            DataPlatformPaymentRequest request = new DataPlatformPaymentRequest(
                event.paymentId().toString(),
                event.orderId(),
                event.userId(),
                event.amount(),
                "SUCCESS",
                event.transactionKey(),
                event.completedAt().atZone(ZoneOffset.UTC)
            );

            dataPlatformClient.sendPaymentData(event.userId(), request);

            log.info("데이터 플랫폼 결제 데이터 전송 완료 - paymentId: {}", event.paymentId());
        } catch (Exception e) {
            // 데이터 플랫폼 전송 실패는 핵심 비즈니스에 영향을 주지 않음
            log.error("데이터 플랫폼 결제 데이터 전송 실패 - paymentId: {}, error: {}",
                event.paymentId(), e.getMessage());
            // TODO: 재시도 큐에 등록하거나 관리자 알림
        }
    }
}
