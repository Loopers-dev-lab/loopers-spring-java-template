package com.loopers.application.order;

import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.order.event.OrderFailureEvent;
import com.loopers.infrastructure.dataplatform.DataPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataPlatformEventListener {

    private final DataPlatform dataPlatform;

    /**
     * 주문 완료 이벤트 처리
     * 트랜잭션 커밋 후에 데이터 플랫폼으로 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("주문 완료 이벤트 수신 - orderId: {}", event.orderId());

        // 주문 데이터 전송
        boolean orderSent = dataPlatform.sendOrderData(
                event.orderId(),
                event.userId(),
                event.totalAmount()
        );

        // 결제 데이터 전송
        boolean paymentSent = dataPlatform.sendPaymentData(
                event.orderId(),
                event.paymentId(),
                event.paymentType()
        );

        if (orderSent && paymentSent) {
            log.info("데이터 플랫폼 전송 완료 - orderId: {}", event.orderId());
        } else {
            log.error("데이터 플랫폼 전송 실패 - orderId: {}", event.orderId());
        }
    }

    /**
     * 주문 실패 이벤트 처리
     * 트랜잭션 커밋 후에 데이터 플랫폼으로 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderFailure(OrderFailureEvent event) {
        log.info("주문 실패 이벤트 수신 - orderId: {}", event.orderId());

        // 주문 데이터 전송
        boolean orderSent = dataPlatform.sendOrderData(
                event.orderId(),
                event.userId(),
                event.totalAmount()
        );

        // 결제 데이터 전송
        boolean paymentSent = dataPlatform.sendPaymentData(
                event.orderId(),
                event.paymentId(),
                event.paymentType()
        );

        if (orderSent && paymentSent) {
            log.info("데이터 플랫폼 전송 완료 - orderId: {}", event.orderId());
        } else {
            log.error("데이터 플랫폼 전송 실패 - orderId: {}", event.orderId());
        }
    }
}
