package com.loopers.application.coupon;

import com.loopers.domain.coupon.event.CouponUsedEvent;
import com.loopers.domain.issuedcoupon.IssuedCouponService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventListener {

    private final IssuedCouponService issuedCouponService;

    /**
     * 쿠폰 사용 처리 이벤트
     *
     * 주문 트랜잭션과 독립적으로 실행
     *
     * 1. 쿠폰 사용 처리 (IssuedCouponService.useCoupon)
     * 2. 이미 사용된 쿠폰이면 스킵 (멱등성)
     * 3. 실패 시 로깅 및 알림 (수동 개입 필요)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponUsage(CouponUsedEvent event) {
        log.info("[쿠폰 사용 처리 시작] userId={}, couponId={}, orderId={}",
                event.userId(), event.couponId(), event.orderId());

        try {
            issuedCouponService.useCoupon(event.userId(), event.couponId());

            log.info("[쿠폰 사용 처리 완료] userId={}, couponId={}, orderId={}",
                    event.userId(), event.couponId(), event.orderId());

        } catch (CoreException e) {
            // 이미 사용된 쿠폰 - 멱등성 보장
            if (e.getErrorType() == ErrorType.BAD_REQUEST &&
                e.getMessage().contains("이미 사용되거나 만료된")) {
                log.warn("[쿠폰 이미 사용됨] couponId={}, orderId={}, message={}",
                        event.couponId(), event.orderId(), e.getMessage());
                return;
            }

            log.error("[쿠폰 사용 처리 실패] couponId={}, orderId={}. 수동 개입 필요",
                    event.couponId(), event.orderId(), e);
            throw e;
        }
    }
}

