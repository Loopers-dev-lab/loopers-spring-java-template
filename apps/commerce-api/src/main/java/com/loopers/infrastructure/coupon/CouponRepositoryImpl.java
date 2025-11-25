package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;
    private final CouponQueryRepository couponQueryRepository;

    @Override
    public Optional<Coupon> save(Coupon coupon) {
        Coupon savedCoupon = couponJpaRepository.save(coupon);
        return Optional.of(savedCoupon);
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return couponJpaRepository.findById(couponId);
    }

    @Override
    public List<Coupon> findByUserId(Long userId) {
        return couponJpaRepository.findByUserId(userId);
    }

    @Override
    public List<Coupon> findByUserIdAndIsUsedFalse(Long userId) {
        return couponJpaRepository.findByUserIdAndIsUsedFalse(userId);
    }

    @Override
    public long useCoupon(Long couponId, Long orderId, Long userId) {
        return couponQueryRepository.useCoupon(couponId, orderId, userId);
    }
}

