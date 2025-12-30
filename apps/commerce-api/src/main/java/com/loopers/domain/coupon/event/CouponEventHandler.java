package com.loopers.domain.coupon.event;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.event.InboxEventService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.stock.event.StockEvents;
import com.loopers.infrastructure.coupon.event.CouponInboxEventRepository;
import com.loopers.support.error.CoreException;
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
    private final CouponInboxEventRepository couponInboxEventRepository;
    private final InboxEventService inboxEventService;

    @Transactional(noRollbackFor = CoreException.class)
    public void handleStockProcessed(StockEvents.Processed event) {
        log.info("CouponEventHandler: StockProcessedEvent 처리 - orderId: {}", event.orderId());

        // Inbox 패턴을 통한 멱등성 체크
        boolean isDuplicate = inboxEventService.checkAndSave(
                couponInboxEventRepository,
                event,
                "stock.v1",
                (eventId, aggregateId, type, topic) -> CouponInboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .topic(topic)
                        .build()
        );
        if (isDuplicate) {
            log.info("Duplicate event detected in Inbox, skipping - eventId: {}, orderId: {}", 
                    event.getEventId(), event.orderId());
            return;
        }

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
        try {
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
        } catch (Exception e) {
            // 쿠폰 사용 실패 시 실패 이벤트 발행
            log.error("쿠폰 사용 실패 - orderId: {}, error: {}", event.orderId(), e.getMessage(), e);
            couponEventPublisher.publishCouponProcessingFailed(new CouponEvents.ProcessingFailed(
                    event.orderId(),
                    event,
                    "쿠폰 처리 실패: " + e.getMessage()
            ));
            // 예외를 다시 던지지 않고 return하여 트랜잭션이 커밋되어 이벤트가 발행되도록 함
            // 보상 트랜잭션은 CouponEvents.ProcessingFailed를 받은 다른 핸들러들이 처리함
            return;
        }
    }

    @Transactional
    public void handlePaymentProcessingFailed(com.loopers.domain.payment.event.PaymentEvents.ProcessingFailed event) {
        log.info("CouponEventHandler: PaymentProcessingFailedEvent 처리 - orderId: {}", event.orderId());

        // Inbox 패턴을 통한 멱등성 체크
        boolean isDuplicate = inboxEventService.checkAndSave(
                couponInboxEventRepository,
                event,
                "payment.v1",
                (eventId, aggregateId, type, topic) -> CouponInboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .topic(topic)
                        .build()
        );
        if (isDuplicate) {
            log.info("Duplicate event detected in Inbox, skipping - eventId: {}, orderId: {}", 
                    event.getEventId(), event.orderId());
            return;
        }

        couponService.rollbackCoupon(event.orderId());
        log.info("쿠폰 원복 성공 - orderId: {}", event.orderId());
        couponEventPublisher.publishCouponCompensated(new CouponEvents.Compensated(event.orderId()));
    }
}

