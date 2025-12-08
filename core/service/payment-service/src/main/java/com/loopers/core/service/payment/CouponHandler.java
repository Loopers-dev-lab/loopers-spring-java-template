package com.loopers.core.service.payment;

import com.loopers.core.domain.order.Coupon;
import com.loopers.core.domain.order.repository.CouponRepository;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.service.payment.event.PgPaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class CouponHandler {

    private final CouponRepository couponRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(PgPaymentEvent pgPaymentEvent) {
        CouponId couponId = pgPaymentEvent.couponId();
        if (couponId.nonEmpty()) {
            Coupon coupon = couponRepository.getById(couponId);
            coupon.use();
            couponRepository.save(coupon);
        }
    }
}
