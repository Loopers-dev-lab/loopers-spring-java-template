package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.user.User;
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
    public List<Coupon> findByUser(User user) {
        return couponJpaRepository.findByUser_Id(user.getId());
    }

    @Override
    public List<Coupon> findByUserAndIsUsedFalse(User user) {
        return couponJpaRepository.findByUser_IdAndIsUsedFalse(user.getId());
    }

    @Override
    public long useCoupon(Long couponId, Long orderId, Long userId) {
        return couponQueryRepository.useCoupon(couponId, orderId, userId);
    }
}

