package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }

    @Override
    public Optional<List<Coupon>> findAllByUserId(Long userId) {
        return couponJpaRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<Coupon> findById(Long couponId) {
        return couponJpaRepository.findById(couponId);
    }
}
