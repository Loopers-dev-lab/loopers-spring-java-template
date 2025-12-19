package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findByIdAndUserIdForUpdate(Long couponId, Long userId) {
        return couponJpaRepository.findByIdAndUserIdForUpdate(couponId, userId);
    }

    @Override
    public Optional<Coupon> findByIdAndUserId(Long couponId, Long userId) {
        return couponJpaRepository.findByIdAndUserId(couponId, userId);
    }
}
