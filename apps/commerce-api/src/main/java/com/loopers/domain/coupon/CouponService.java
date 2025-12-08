package com.loopers.domain.coupon;

import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponService {

  private final CouponRepository couponRepository;
  private final Clock clock;

  public Optional<Coupon> findById(Long id) {
    return couponRepository.findById(id);
  }

  @Transactional
  public Coupon useCoupon(Long couponId, Long userId, Long orderId) {
    Objects.requireNonNull(couponId, "couponId는 null일 수 없습니다.");
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(orderId, "orderId는 null일 수 없습니다.");

    Coupon coupon = couponRepository.findByIdWithPolicyAndLock(couponId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

    if (!coupon.isOwnedBy(userId)) {
      throw new CoreException(ErrorType.COUPON_NOT_OWNED);
    }

    coupon.toUsed(orderId, LocalDateTime.now(clock));
    return couponRepository.save(coupon);
  }

  @Transactional
  public void restoreCoupon(Long couponId) {
    if(couponId == null){
      return;
    }

    Coupon coupon = couponRepository.findByIdWithPolicyAndLock(couponId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

    coupon.toAvailable();
    couponRepository.save(coupon);
  }

  @Transactional(readOnly = true)
  public Money calculateDiscount(Long couponId, Money orderAmount) {
    Objects.requireNonNull(couponId, "couponId는 null일 수 없습니다.");
    Objects.requireNonNull(orderAmount, "orderAmount는 null일 수 없습니다.");

    Coupon coupon = couponRepository.findByIdWithPolicy(couponId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

    return coupon.calculateDiscount(orderAmount);
  }
}
