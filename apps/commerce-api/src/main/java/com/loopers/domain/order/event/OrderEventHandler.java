package com.loopers.domain.order.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 관련 이벤트 핸들러
 * SAGA 패턴의 주문 완료/실패 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;

    @Transactional
    public void handlePaymentProcessed(PaymentEvents.Processed event) {
        log.info("OrderEventHandler: PaymentProcessedEvent 처리 - orderId: {}", event.orderId());

        log.info("Saga 최종 성공 처리 - orderId: {}", event.orderId());
        var order = orderService.saveSuccessOrder(event.orderId());

        // 주문 완료 이벤트 발행 (데이터 플랫폼 전송용)
        orderEventPublisher.publishOrderConfirmed(
                new OrderEvents.Confirmed(
                        order.getId(),
                        order.getUserId(),
                        order.getOrderStatus().name()
                )
        );
    }

    @Transactional
    public void handleStockProcessingFailed(StockEvents.ProcessingFailed event) {
        log.info("OrderEventHandler: StockProcessingFailedEvent 처리 - orderId: {}, reason: {}",
                event.orderId(), event.reason());

        log.info("Saga 최종 실패 처리 (Stock) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

    @Transactional
    public void handleCouponProcessingFailed(CouponEvents.ProcessingFailed event) {
        log.info("OrderEventHandler: CouponProcessingFailedEvent 처리 - orderId: {}, reason: {}",
                event.orderId(), event.reason());

        log.info("Saga 최종 실패 처리 (Coupon) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

    @Transactional
    public void handlePaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        log.info("OrderEventHandler: PaymentProcessingFailedEvent 처리 - orderId: {}, reason: {}",
                event.orderId(), event.reason());

        log.info("Saga 최종 실패 처리 (Payment) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }
}

