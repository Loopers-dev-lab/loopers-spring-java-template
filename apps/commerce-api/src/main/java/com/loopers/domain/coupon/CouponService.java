package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon getCouponByIdAndUserId(Long couponId, Long userId) {
        return couponRepository.findCouponByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
    }

    public int calculateDiscountPrice(Coupon coupon, int originalTotalPrice) {
        if(coupon.isUsed()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        return coupon.calculateDiscountPrice(originalTotalPrice);
    }

    public void usedCoupon(Coupon coupon) {
        coupon.use();
    }

    @Transactional
    public void restoreCoupon(Coupon coupon) {
        coupon.restore();
        couponRepository.saveCoupon(coupon);
    }

    @Transactional
    public void saveCoupon(Coupon coupon) {
        couponRepository.saveCoupon(coupon);
    }
}
