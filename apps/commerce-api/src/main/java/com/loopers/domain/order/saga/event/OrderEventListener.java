package com.loopers.domain.order.saga.event;

import com.loopers.domain.coupon.event.CouponProcessingFailedEvent;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentProcessedEvent;
import com.loopers.domain.payment.event.PaymentProcessingFailedEvent;
import com.loopers.domain.stock.event.StockProcessingFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderService orderService;

    // Saga Success Listener
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Saga 최종 성공 처리 - orderId: {}", event.orderId());
        orderService.saveSuccessOrder(event.orderId());
    }

    // Saga Failure Listeners
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockProcessingFailed(StockProcessingFailedEvent event) {
        log.error("Saga 최종 실패 처리 (Stock) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcessingFailed(CouponProcessingFailedEvent event) {
        log.error("Saga 최종 실패 처리 (Coupon) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessingFailed(PaymentProcessingFailedEvent event) {
        log.error("Saga 최종 실패 처리 (Payment) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

}
