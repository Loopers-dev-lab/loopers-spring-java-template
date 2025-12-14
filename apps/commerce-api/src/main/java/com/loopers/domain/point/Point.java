package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "point")
public class Point extends BaseEntity {

  @Getter
  @Column(name = "ref_user_id", nullable = false, unique = true)
  private Long userId;

  @Embedded
  private PointAmount amount;

  protected Point() {
  }

  private Point(Long userId, PointAmount amount) {
    validateUserId(userId);
    validateAmount(amount);
    this.userId = userId;
    this.amount = amount;
  }

  public static Point of(Long userId, Long amount) {
    return new Point(userId, PointAmount.of(amount));
  }

  public static Point of(Long userId, PointAmount amount) {
    return new Point(userId, amount);
  }

  public static Point zero(Long userId) {
    return new Point(userId, PointAmount.zero());
  }

  private void validateUserId(Long userId) {
    if (userId == null) {
      throw new CoreException(ErrorType.INVALID_POINT_USER_EMPTY);
    }
  }

  private void validateAmount(PointAmount amount) {
    if (amount == null) {
      throw new CoreException(ErrorType.INVALID_POINT_AMOUNT_EMPTY);
    }
  }

  public void charge(Long chargeAmount) {
    this.amount = this.amount.add(chargeAmount);
  }

  public PointDeductionResult deduct(Money requestAmount) {
    PointDeductionResult result = calculateDeduction(requestAmount);

    if (result.deductedAmount().getValue() > 0) {
      this.amount = this.amount.subtract(result.deductedAmount().getValue());
    }

    return result;
  }

  public Long getAmountValue() {
    return amount.getValue();
  }

  public PointDeductionResult calculateDeduction(Money requestAmount) {
    if (requestAmount == null) {
      throw new CoreException(ErrorType.INVALID_DEDUCT_AMOUNT, "차감 금액은 null일 수 없습니다.");
    }

    Long currentBalance = this.amount.getValue();
    Long requestValue = requestAmount.getValue();
    Long deductedValue = Math.min(currentBalance, requestValue);
    Long remainingValue = requestValue - deductedValue;

    return PointDeductionResult.of(Money.of(deductedValue), Money.of(remainingValue));
  }

}
