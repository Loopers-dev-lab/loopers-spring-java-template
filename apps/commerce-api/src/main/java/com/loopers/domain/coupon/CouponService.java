package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CouponService {

    private final CouponRepository couponRepository;

    public Coupon getValidCoupon(Long couponId) {

        if( couponId == null ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 쿠폰입니다.");
        }

        Coupon coupon = couponRepository.findValidCoupon(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "유효하지 않은 쿠폰입니다"));

        if( !coupon.isValidNow() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰을 사용할 수 있는 기간이 아닙니다");
        }

        return coupon;
    }
}
