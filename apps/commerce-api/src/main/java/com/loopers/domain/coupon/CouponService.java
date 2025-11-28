package com.loopers.domain.coupon;

import com.loopers.domain.common.vo.Price;
import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class CouponService {
    private final CouponRepository couponRepository;

    @Transactional
    public Coupon issuePercentage(Long userId, int discountPercent) {
        Coupon coupon = Coupon.issuePercentage(userId, discountPercent);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon issueFixed(Long userId, int discountAmount) {
        Coupon coupon = Coupon.issueFixed(userId, discountAmount);
        return couponRepository.save(coupon);
    }

    @Transactional
    public DiscountResult applyCoupon(Long couponId, Long userId, Price totalAmount) {
        Coupon coupon = couponRepository.findByIdAndUserIdForUpdate(couponId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
        return coupon.applyDiscount(totalAmount);
    }
}
