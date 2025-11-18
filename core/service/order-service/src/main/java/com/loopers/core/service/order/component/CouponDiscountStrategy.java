package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.Coupon;
import com.loopers.core.domain.order.repository.CouponRepository;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.vo.PayAmount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CouponDiscountStrategy implements PayAmountDiscountStrategy {

    private final CouponRepository couponRepository;

    @Override
    @Transactional
    public PayAmount discount(PayAmount payAmount, CouponId couponId) {
        Coupon coupon = couponRepository.getByIdWithLock(couponId);
        PayAmount discountedAmount = coupon.discount(payAmount);
        coupon.use();
        couponRepository.save(coupon);

        return discountedAmount;
    }
}
