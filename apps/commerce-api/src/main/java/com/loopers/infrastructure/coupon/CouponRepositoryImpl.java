package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    @Override
    public Optional<Coupon> findCouponByIdAndUserId(Long couponId, Long userId) {
        return couponJpaRepository.findByIdAndUserIdWithLock(couponId, userId);
    }

    @Override
    public void saveCoupon(Coupon coupon) {
        couponJpaRepository.save(coupon);
    }
}

