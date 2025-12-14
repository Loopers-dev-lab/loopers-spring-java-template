package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
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
  public Optional<Coupon> findByIdWithPolicy(Long id) {
    return jpaRepository.findByIdWithPolicy(id);
  }

  @Override
  public Optional<Coupon> findByIdWithPolicyAndLock(Long id) {
    return jpaRepository.findByIdWithPolicyAndLock(id);
  }
}
