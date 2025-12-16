package com.loopers.application.coupon;

import com.loopers.domain.coupon.event.CouponUsedEvent;
import com.loopers.domain.issuedcoupon.IssuedCoupon;
import com.loopers.domain.issuedcoupon.IssuedCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventListener {

    private final IssuedCouponService issuedCouponService;

    /**
     * 쿠폰 사용 처리
     * */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleCouponUsage(CouponUsedEvent event) {
        log.info("쿠폰 사용 처리 이벤트 시작 - CouponId: {}, UserId: {}", event.couponId(), event.userId());

        // 주문 트랜잭션 내에서 쿠폰 상태 변경
        IssuedCoupon issuedCoupon = issuedCouponService
                .getIssuedCoupon(event.userId(), event.couponId());
        issuedCoupon.useCoupon();

        log.info("쿠폰 사용 처리 이벤트 완료 - CouponId: {}, UserId: {}", event.couponId(), event.userId());
    }
}
