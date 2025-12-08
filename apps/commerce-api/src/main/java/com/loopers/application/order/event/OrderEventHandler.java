package com.loopers.application.order.event;

import com.loopers.application.coupon.CouponService;
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.domain.payment.event.PaymentRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 이벤트 핸들러
 * - 주문 생성 후 후속 처리 담당
 * - 쿠폰 사용, 결제 요청 등
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 생성 이벤트 처리
     * - 트랜잭션 커밋 후 실행 (동기)
     * - 쿠폰 사용 처리
     * - 결제 요청 이벤트 발행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("주문 생성 이벤트 처리 시작 - orderId: {}, eventId: {}",
            event.orderId(), event.eventId());

        try {
            // 1. 쿠폰 사용 처리 (있는 경우에만)
            if (event.hasCoupon()) {
                log.info("쿠폰 사용 처리 시작 - userCouponId: {}", event.userCouponId());
                couponService.useCoupon(event.userCouponId());
                log.info("쿠폰 사용 완료 - userCouponId: {}", event.userCouponId());
            }

            // 2. 결제 요청 이벤트 발행 (비동기로 처리됨)
            PaymentRequestedEvent paymentEvent = PaymentRequestedEvent.from(event);
            eventPublisher.publishEvent(paymentEvent);
            log.info("결제 요청 이벤트 발행 - orderId: {}", event.orderId());

        } catch (Exception e) {
            log.error("주문 생성 이벤트 처리 실패 - orderId: {}, error: {}",
                event.orderId(), e.getMessage(), e);
            // TODO: 실패 보상 처리 (쿠폰 복구 등)
            // 현재는 로그만 남기고 진행 (주문은 이미 저장됨)
        }
    }
}
