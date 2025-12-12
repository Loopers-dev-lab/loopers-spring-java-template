package com.loopers.domain.order;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.money.Money;
import com.loopers.domain.point.PointDeductionResult;
import com.loopers.domain.point.PointService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPaymentCalculator {

  private final CouponService couponService;
  private final PointService pointService;

  public OrderPaymentCalculation calculate(Long userId, Long couponId, Money totalAmount) {
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
    Objects.requireNonNull(totalAmount, "totalAmount는 null일 수 없습니다.");

    Money discountAmount = calculateCouponDiscount(couponId, userId, totalAmount);
    Money amountAfterDiscount = totalAmount.subtract(discountAmount);
    PointDeductionResult deduction = pointService.calculateDeduction(userId, amountAfterDiscount);

    return OrderPaymentCalculation.of(
        discountAmount,
        amountAfterDiscount,
        deduction.deductedAmount(),
        deduction.remainingToPay()
    );
  }

  private Money calculateCouponDiscount(Long couponId, Long userId, Money totalAmount) {
    if (couponId == null) {
      return Money.zero();
    }
    return couponService.calculateDiscount(couponId, userId, totalAmount);
  }
}
