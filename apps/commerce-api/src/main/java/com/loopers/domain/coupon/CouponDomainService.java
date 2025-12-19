package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponDomainService {

    private final CouponRepository couponRepository;

    @Transactional
    public Coupon issueCoupon(String userId, String name, long discountAmount) {
        Coupon coupon = Coupon.create(userId, name, discountAmount);
        return couponRepository.save(coupon);
    }

    @Transactional
    public void useCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        ErrorMessage.COUPON_NOT_FOUND
                ));
        coupon.use();
        couponRepository.save(coupon);
        log.info("쿠폰 사용 완료: couponId={}, userId={}", couponId, coupon.getUserId());
    }

    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        ErrorMessage.COUPON_NOT_FOUND
                ));
    }

    @Transactional(readOnly = true)
    public List<Coupon> getCoupons(String userId) {
        return couponRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getUsableCoupons(String userId) {
        return couponRepository.findByUserIdAndStatus(userId, CouponStatus.ISSUED);
    }

    @Transactional(readOnly = true)
    public Coupon validateAndGetCoupon(String userId, Long couponId) {
        Coupon coupon = getCoupon(couponId);

        if (!coupon.getUserId().equals(userId)) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    ErrorMessage.COUPON_NOT_OWNER
            );
        }

        if (!coupon.isUsable()) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    ErrorMessage.COUPON_ALREADY_USED
            );
        }

        return coupon;
    }
}
