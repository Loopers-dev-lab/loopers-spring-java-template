package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * CouponRepository의 JPA 구현체.
 * <p>
 * Spring Data JPA를 활용하여 Coupon 엔티티의
 * 영속성 작업을 처리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository couponJpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Coupon save(Coupon coupon) {
        return couponJpaRepository.save(coupon);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Coupon> findByCode(String code) {
        return couponJpaRepository.findByCode(code);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Coupon> findById(Long couponId) {
        return couponJpaRepository.findById(couponId);
    }
}

