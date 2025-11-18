package com.loopers.domain.coupon;

import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    public Coupon getCouponWithOptimisticLock(Long id) {
        return couponRepository.findByIdWithOptimisticLock(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
    }

    @Transactional
    public Coupon getCouponWithPessimisticLock(Long id) {
        return couponRepository.findByIdWithPessimisticLock(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
    }

    public void validateCouponUsable(Coupon coupon, User user) {
        if (!coupon.getUser().equals(user)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "본인의 쿠폰만 사용할 수 있습니다.");
        }
        if (!coupon.canUse()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }
    }

    @Transactional
    public Coupon save(Coupon coupon) {
        return couponRepository.save(coupon);
    }
}
