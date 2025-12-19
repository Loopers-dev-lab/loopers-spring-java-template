package com.loopers.interfaces.listener;

import com.loopers.application.order.OrderEvent;
import com.loopers.domain.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponEventListener {

    private final CouponService couponService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(OrderEvent.CouponUsed event) {
        if (event.couponId() != null) {
            couponService.useCoupon(event.couponId(), event.userId());
        }
    }
}

