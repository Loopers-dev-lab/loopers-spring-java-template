package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 쿠폰을 생성합니다.
     */
    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    /**
     * 쿠폰을 조회합니다.
     */
    @Transactional(readOnly = true)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
    }

    /**
     * 모든 쿠폰을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    /**
     * 사용자에게 쿠폰을 발급합니다.
     */
    @Transactional
    public UserCoupon issueCouponToUser(String userId, Long couponId) {
        Coupon coupon = getCoupon(couponId);

        UserCoupon userCoupon = UserCoupon.builder()
            .userId(userId)
            .coupon(coupon)
            .build();

        return userCouponRepository.save(userCoupon);
    }

    /**
     * 사용자의 쿠폰을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<UserCoupon> getUserCoupons(String userId) {
        return userCouponRepository.findByUserId(userId);
    }

    /**
     * 사용자의 사용 가능한 쿠폰을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<UserCoupon> getAvailableUserCoupons(String userId) {
        return userCouponRepository.findByUserIdAndIsAvailable(userId);
    }

    /**
     * 사용자 쿠폰을 조회합니다 (락 사용).
     */
    @Transactional
    public UserCoupon getUserCouponWithLock(Long userCouponId) {
        return userCouponRepository.findByIdWithLock(userCouponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 쿠폰을 찾을 수 없습니다."));
    }

    /**
     * 쿠폰을 사용합니다 (동시성 제어 포함).
     */
    @Transactional
    public void useCoupon(Long userCouponId) {
        UserCoupon userCoupon = getUserCouponWithLock(userCouponId);

        if (!userCoupon.isAvailable()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }

        userCoupon.use();
        userCouponRepository.save(userCoupon);
    }
}
