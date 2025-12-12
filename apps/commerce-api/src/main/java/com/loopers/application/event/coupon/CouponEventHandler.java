package com.loopers.application.event.coupon;

import com.loopers.application.order.OrderCreatedEvent;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CouponEventHandler {
    private final CouponRepository couponRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        Long couponId = event.couponId();
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다.")
        );

        coupon.useCoupon();
    }
}
