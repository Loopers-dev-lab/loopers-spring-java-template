package com.loopers.domain.analytics.event;

import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데이터 플랫폼 전송 이벤트 리스너
 * TODO: 실제 데이터 플랫폼 전송 로직 구현 필요
 */
@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventListener.class);

    /**
     * 주문 완료 이벤트 처리
     * 데이터 플랫폼에 주문 완료 정보 전송
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderConfirmed(OrderEvents.Confirmed event) {
        log.info("AnalyticsEventListener: OrderConfirmedEvent 수신 - orderId: {}, userId: {}, status: {}", 
                event.orderId(), event.userId(), event.orderStatus());
        
        // TODO: 데이터 플랫폼 전송 로직 구현
        // 예: Kafka로 전송, 외부 API 호출 등
    }

    /**
     * 결제 완료 이벤트 처리
     * 데이터 플랫폼에 결제 완료 정보 전송
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessed(PaymentEvents.Processed event) {
        log.info("AnalyticsEventListener: PaymentProcessedEvent 수신 - orderId: {}", event.orderId());
        
        // TODO: 데이터 플랫폼 전송 로직 구현
        // 예: Kafka로 전송, 외부 API 호출 등
    }
}

