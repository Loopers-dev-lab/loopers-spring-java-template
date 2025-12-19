package com.loopers.domain.coupon;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.coupon.event.CouponConsumeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠폰 도메인 이벤트 핸들러 (Infrastructure Layer)
 *
 * @author hyunjikoh
 * @since 2025. 12. 10.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CouponEventHandler {
    private final CouponService couponService;

    /**
     * 쿠폰 사용 이벤트 처리 (핵심 비즈니스 로직)
     * <p>
     * BEFORE_COMMIT으로 주문 트랜잭션과 함께 처리하여 일관성 보장
     * 쿠폰 사용 실패 시 전체 주문 트랜잭션 롤백
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleCouponConsume(CouponConsumeEvent event) {
        Objects.requireNonNull(event, "쿠폰 이벤트가 null 입니다.");

        CouponEntity coupon = couponService.getCouponByIdAndUserId(event.couponId(), event.userId());
        log.debug("쿠폰 사용 처리 시작 - orderNumber={}, userId={}, couponId={}",
                event.orderNumber(), coupon.getUserId(), coupon.getId());

        couponService.consumeCoupon(coupon);

        log.debug("쿠폰 사용 처리 완료 - orderNumber={}",
                event.orderNumber());
    }
}
