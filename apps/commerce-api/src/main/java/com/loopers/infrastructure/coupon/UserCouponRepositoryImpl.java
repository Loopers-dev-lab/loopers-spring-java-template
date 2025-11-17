package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponJpaRepository userCouponJpaRepository;

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return userCouponJpaRepository.save(userCoupon);
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return userCouponJpaRepository.findById(id);
    }

    @Override
    public Optional<UserCoupon> findByIdWithLock(Long id) {
        return userCouponJpaRepository.findByIdWithLock(id);
    }

    @Override
    public List<UserCoupon> findByUserId(String userId) {
        return userCouponJpaRepository.findByUserId(userId);
    }

    @Override
    public List<UserCoupon> findByUserIdAndIsAvailable(String userId) {
        return userCouponJpaRepository.findByUserIdAndIsAvailable(userId);
    }
}
