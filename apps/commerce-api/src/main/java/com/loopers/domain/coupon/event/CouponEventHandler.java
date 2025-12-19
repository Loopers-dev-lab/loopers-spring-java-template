package com.loopers.domain.coupon.event;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.stock.event.StockEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 쿠폰 관련 이벤트 핸들러
 * SAGA 패턴의 쿠폰 적용 및 보상 트랜잭션 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventHandler {

    private final CouponService couponService;
    private final OrderService orderService;
    private final CouponEventPublisher couponEventPublisher;

    @Transactional
    public void handleStockProcessed(StockEvents.Processed event) {
        log.info("CouponEventHandler: StockProcessedEvent 처리 - orderId: {}", event.orderId());

        var couponIds = event.originalEvent().couponIds();
        Long userId = event.originalEvent().userId();
        BigDecimal totalAmount = event.originalEvent().totalAmount();

        if (couponIds == null || couponIds.isEmpty()) {
            log.info("사용할 쿠폰 없음 - orderId: {}", event.orderId());
            couponEventPublisher.publishCouponProcessed(new CouponEvents.Processed(
                    event.orderId(),
                    userId,
                    BigDecimal.ZERO,
                    event
            ));
            return;
        }

        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        // 각 쿠폰에 대해 할인 금액 계산
        for (Long couponId : couponIds) {
            BigDecimal discount = couponService.useCoupon(
                    event.orderId(),
                    userId,
                    totalAmount,
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
    }

    @Transactional
    public void handlePaymentProcessingFailed(com.loopers.domain.payment.event.PaymentEvents.ProcessingFailed event) {
        log.info("CouponEventHandler: PaymentProcessingFailedEvent 처리 - orderId: {}", event.orderId());

        couponService.rollbackCoupon(event.orderId());
        log.info("쿠폰 원복 성공 - orderId: {}", event.orderId());
        couponEventPublisher.publishCouponCompensated(new CouponEvents.Compensated(event.orderId()));
    }
}

