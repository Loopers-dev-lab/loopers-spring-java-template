package com.loopers.domain.order.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessed(PaymentEvents.Processed event) {
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

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockProcessingFailed(StockEvents.ProcessingFailed event) {
        log.info("Saga 최종 실패 처리 (Stock) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcessingFailed(CouponEvents.ProcessingFailed event) {
        log.info("Saga 최종 실패 처리 (Coupon) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        log.info("Saga 최종 실패 처리 (Payment) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

}
