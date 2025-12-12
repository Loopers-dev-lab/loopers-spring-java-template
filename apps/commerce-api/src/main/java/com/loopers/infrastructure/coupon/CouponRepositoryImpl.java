package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 쿠폰 레포지토리 구현체
 */
@Repository
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponJpaRepository;

    public CouponRepositoryImpl(CouponJpaRepository couponJpaRepository) {
        this.couponJpaRepository = couponJpaRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity entity = couponJpaRepository.findById(coupon.getId())
                .map(existing -> {
                    existing.updateFrom(coupon);
                    return existing;
                })
                .orElse(CouponEntity.from(coupon));

        CouponEntity saved = couponJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Coupon> findById(Long id) {
        return couponJpaRepository.findById(id)
                .map(CouponEntity::toDomain);
    }

    @Override
    public Optional<Coupon> findByIdWithLock(Long id) {
        return couponJpaRepository.findByIdWithLock(id)
                .map(CouponEntity::toDomain);
    }

    @Override
    public List<Coupon> findByUserId(String userId) {
        return couponJpaRepository.findByUserId(userId).stream()
                .map(CouponEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Coupon> findAvailableCouponsByUserId(String userId) {
        return couponJpaRepository.findAvailableCouponsByUserId(userId, LocalDateTime.now()).stream()
                .map(CouponEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Coupon coupon) {
        couponJpaRepository.findById(coupon.getId())
                .ifPresent(couponJpaRepository::delete);
    }

    @Override
    public boolean existsById(Long id) {
        return couponJpaRepository.existsById(id);
    }
}
