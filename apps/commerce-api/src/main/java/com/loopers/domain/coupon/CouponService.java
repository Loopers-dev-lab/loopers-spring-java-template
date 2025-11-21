package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public Optional<Coupon> findCouponByIdAndUserId(Long couponId, Long userId) {
        return couponRepository.findCouponByIdAndUserId(couponId, userId);
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
    public void saveCoupon(Coupon coupon) {
        couponRepository.saveCoupon(coupon);
    }
}
