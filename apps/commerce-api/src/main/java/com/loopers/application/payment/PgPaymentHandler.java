package com.loopers.application.payment;

import com.loopers.domain.order.*;
import com.loopers.infrastructure.payment.PaymentCallbackUrlGenerator;
import com.loopers.infrastructure.pg.PgPaymentExecutor;
import com.loopers.infrastructure.pg.PgV1Dto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 생성 이벤트를 받아 PG 결제 요청을 처리하는 핸들러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PgPaymentHandler implements OrderCreatedEventHandler {
    private final PgPaymentExecutor pgPaymentExecutor;
    private final OrderService orderService;
    private final PaymentCallbackUrlGenerator callbackUrlGenerator;

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventTaskExecutor")
    public void handleOrderCreated(OrderEvent.OrderCreatedEvent event) {
        log.info("OrderCreatedEvent 수신. in {}.\nevent: {}", this.getClass().getSimpleName(), event);
        if (event == null) {
            log.warn("null event 수신. in {}. 무시합니다.", this.getClass().getSimpleName());
            return;
        }
        if (event.getPaymentMethod() != PaymentMethod.PG) {
            log.info("PG 결제 방식이 아님. in {}. 무시합니다. paymentMethod: {}", this.getClass().getSimpleName(), event.getPaymentMethod());
            return;
        }

        try {
            Order order = orderService.getOrderById(event.getOrderId());
            if (order.getStatus() != OrderStatus.PENDING) {
                log.warn("이미 처리된 주문 - orderId: {}, status: {}",
                        event.getOrderId(), order.getStatus());
                return;
            }

            if (order.getCardType() == null || order.getCardNo() == null) {
                log.warn("결제 정보가 없는 주문 - orderId: {}", event.getOrderId());
                return;
            }

            String callbackUrl = order.getCallbackUrl();
            if (callbackUrl == null) {
                callbackUrl = callbackUrlGenerator.generateCallbackUrl(event.getOrderPublicId());
                orderService.setPgPaymentInfo(
                        event.getOrderId(),
                        order.getCardType(),
                        order.getCardNo(),
                        callbackUrl
                );
            }

            PgV1Dto.PgPaymentRequest pgRequest = new PgV1Dto.PgPaymentRequest(
                    event.getOrderPublicId(),
                    order.getCardType(),
                    order.getCardNo(),
                    (long) event.getFinalPrice().amount(),
                    callbackUrl
            );

            // PG 결제 요청 (비동기)
            pgPaymentExecutor.requestPaymentAsync(pgRequest)
                    .thenAccept(pgResponse -> {
                        // 주문 상태 업데이트
                        orderService.updateStatusByPgResponse(event.getOrderId(), pgResponse);
                        log.info("PG 결제 요청 성공 - orderId: {}, transactionKey: {}", event.getOrderId(), pgResponse.transactionKey());
                    })
                    .exceptionally(throwable -> {
                        log.error("PG 결제 요청 실패 - orderId: {}", event.getOrderId(), throwable);
                        return null;
                    });

        } catch (Exception e) {
            log.error("PG 결제 요청 처리 실패 - orderId: {}", event.getOrderId(), e);
            // PG 실패는 주문에 영향을 주지 않음
        }
    }
}

