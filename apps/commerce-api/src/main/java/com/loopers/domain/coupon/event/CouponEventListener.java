package com.loopers.domain.coupon.event;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventListener {

    private final CouponService couponService;
    private final OrderService orderService;
    private final CouponEventPublisher couponEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockProcessed(StockEvents.Processed event) {
        log.info("CouponEventListener: StockProcessedEvent 수신 - orderId: {}", event.orderId());

        var request = event.originalEvent().request();
        Long userId = event.originalEvent().userId();
        BigDecimal totalPrice = event.originalEvent().totalPrice();
        
        if (request.couponIds() == null || request.couponIds().isEmpty()) {
            log.info("사용할 쿠폰 없음 - orderId: {}", event.orderId());
            couponEventPublisher.publishCouponProcessed(new CouponEvents.Processed(
                event.orderId(), 
                userId,
                BigDecimal.ZERO,
                event
            ));
            return;
        }

        try {
            BigDecimal totalDiscountAmount = BigDecimal.ZERO;
            
            // 각 쿠폰에 대해 할인 금액 계산
            for (Long couponId : request.couponIds()) {
                BigDecimal discount = couponService.useCoupon(
                    event.orderId(), 
                    userId, 
                    totalPrice,
                    couponId
                );
                totalDiscountAmount = totalDiscountAmount.add(discount);
            }
            
            // 주문에 할인 금액 반영
            if (totalDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                orderService.applyDiscount(event.orderId(), totalDiscountAmount);
            }
            
            log.info("쿠폰 사용 성공 - orderId: {}, totalDiscountAmount: {}", event.orderId(), totalDiscountAmount);
            couponEventPublisher.publishCouponProcessed(new CouponEvents.Processed(
                event.orderId(), 
                userId,
                totalDiscountAmount,
                event
            ));
        } catch (Exception e) {
            log.error("쿠폰 사용 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage());
            couponEventPublisher.publishCouponProcessingFailed(new CouponEvents.ProcessingFailed(
                event.orderId(), 
                event,  // 재고 원복을 위해 포함
                e.getMessage()
            ));
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
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
