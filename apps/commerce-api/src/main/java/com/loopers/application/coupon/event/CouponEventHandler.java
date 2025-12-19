package com.loopers.application.coupon.event;

import com.loopers.domain.coupon.CouponDomainService;
import com.loopers.domain.coupon.event.CouponUsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventHandler {

    private final CouponDomainService couponDomainService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponUsed(CouponUsedEvent event) {
        log.info("쿠폰 사용 이벤트 수신 (AFTER_COMMIT): couponId={}, orderId={}",
                event.getCouponId(), event.getOrderId());
        try {
            couponDomainService.useCoupon(event.getCouponId());
            log.info("쿠폰 사용 처리 완료: couponId={}", event.getCouponId());
        } catch (Exception e) {
            log.error("쿠폰 사용 처리 실패: couponId={}, error={}",
                    event.getCouponId(), e.getMessage(), e);
        }
    }
}
