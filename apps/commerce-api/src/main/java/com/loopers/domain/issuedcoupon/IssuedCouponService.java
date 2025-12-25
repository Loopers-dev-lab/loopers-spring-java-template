package com.loopers.domain.issuedcoupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class IssuedCouponService {
    private final IssuedCouponRepository issuedCouponRepository;


    public IssuedCoupon getIssuedCouponByUser(Long userId, Long couponId) {

        if( couponId == null ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 쿠폰입니다.");
        }

        IssuedCoupon issuedCoupon = issuedCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰에 대한 사용 권한이 없습니다"));

        if( issuedCoupon.getStatus() != CouponStatus.USABLE ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용되거나 만료된 쿠폰입니다");
        }

        return issuedCoupon;
    }

    public IssuedCoupon getIssuedCoupon(Long userId, Long couponId) {
        return issuedCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰에 대한 사용 권한이 없습니다"));
    }

    public void useCoupon(Long userId, Long couponId) {
        IssuedCoupon issuedCoupon = issuedCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰에 대한 사용 권한이 없습니다"));

        if( issuedCoupon.getStatus() != CouponStatus.USABLE ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용되거나 만료된 쿠폰입니다");
        }

        issuedCoupon.useCoupon();
    }
}
