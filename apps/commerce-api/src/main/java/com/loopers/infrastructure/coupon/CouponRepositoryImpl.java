package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CouponRepositoryImpl implements CouponRepository {

  private final CouponJpaRepository couponJpaRepository;

  @Override
  public Coupon save(Coupon coupon) {
    return couponJpaRepository.save(coupon);
  }

  @Override
  public Optional<Coupon> findById(Long id) {
    return couponJpaRepository.findById(id);
  }
}
