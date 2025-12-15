package com.loopers.domain.order.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

    /**
     * 재고 처리 실패 핸들러
     * @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) 사용: 트랜잭션과 무관하게 즉시 실행 (트랜잭션 롤백 후에도 이벤트가 발행될 수 있음)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockProcessingFailed(StockEvents.ProcessingFailed event) {
        log.info("Saga 최종 실패 처리 (Stock) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

    /**
     * 쿠폰 처리 실패 핸들러
     * @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) 사용: 트랜잭션과 무관하게 즉시 실행
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcessingFailed(CouponEvents.ProcessingFailed event) {
        log.info("Saga 최종 실패 처리 (Coupon) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

    /**
     * 결제 처리 실패 핸들러
     * @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) 사용: 트랜잭션과 무관하게 즉시 실행
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        log.info("Saga 최종 실패 처리 (Payment) - orderId: {}, reason: {}", event.orderId(), event.reason());
        orderService.saveFailedOrder(event.orderId(), event.reason());
    }

}
