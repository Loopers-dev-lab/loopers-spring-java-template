package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository jpaRepository;

    @Override
    public Coupon save(Coupon coupon) {
        return jpaRepository.save(coupon);
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Coupon> findByIdWithOptimisticLock(Long id) {
        return jpaRepository.findByIdWithOptimisticLock(id);
    }

    @Override
    public Optional<Coupon> findByIdWithPessimisticLock(Long id) {
        return jpaRepository.findByIdWithPessimisticLock(id);
    }

    @Override
    public Optional<Coupon> findByIdAndUser(Long id, User user) {
        return jpaRepository.findByIdAndUser(id, user);
    }
}
