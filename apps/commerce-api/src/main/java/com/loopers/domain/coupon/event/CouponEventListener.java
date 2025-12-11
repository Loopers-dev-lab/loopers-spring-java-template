package com.loopers.domain.coupon.event;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
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
public class CouponEventListener {

    private static final Logger log = LoggerFactory.getLogger(CouponEventListener.class);

    private final CouponService couponService;
    private final OrderService orderService;
    private final CouponEventPublisher couponEventPublisher;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockProcessed(StockEvents.Processed event) {
        log.info("CouponEventListener: StockProcessedEvent 수신 - orderId: {}", event.orderId());

        var request = event.originalEvent().request();
        if (request.couponIds() == null || request.couponIds().isEmpty()) {
            log.info("사용할 쿠폰 없음 - orderId: {}", event.orderId());
            couponEventPublisher.publishCouponProcessed(new CouponEvents.Processed(event.orderId(), event));
            return;
        }

        try {
            Order order = orderService.findOrderById(event.orderId());
            for (Long couponId : request.couponIds()) {
                couponService.useCoupon(order, couponId);
            }
            log.info("쿠폰 사용 성공 - orderId: {}", event.orderId());
            couponEventPublisher.publishCouponProcessed(new CouponEvents.Processed(event.orderId(), event));
        } catch (Exception e) {
            log.error("쿠폰 사용 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage());
            couponEventPublisher.publishCouponProcessingFailed(new CouponEvents.ProcessingFailed(event.orderId(), e.getMessage()));
        }
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentProcessingFailed(PaymentEvents.ProcessingFailed event) {
        log.info("CouponEventListener: PaymentProcessingFailedEvent 수신 - orderId: {}", event.orderId());
        try {
            couponService.rollbackCoupon(event.orderId());
            log.info("쿠폰 원복 성공 - orderId: {}", event.orderId());
            couponEventPublisher.publishCouponCompensated(new CouponEvents.Compensated(event.orderId()));
        } catch (Exception e) {
            log.error("쿠폰 원복 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage());
            // 보상 트랜잭션 실패에 대한 처리 전략 필요 (e.g., 재시도, 로깅, 알림)
        }
    }
}
