package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
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

  /**
   * 포인트를 차감하고 결과를 반환.
   * 잔액보다 요청 금액이 크면 잔액만큼만 차감.
   *
   * @param amount 차감하고자 하는 금액
   * @return PointDeductionResult (실제 차감 금액, 남은 결제 필요 금액)
   */
  public PointDeductionResult deduct(Long amount) {
    Long currentBalance = this.amount.getValue();
    Long deductedAmount = Math.min(currentBalance, amount);
    Long remainingToPay = amount - deductedAmount;

    if (deductedAmount > 0) {
      this.amount = this.amount.subtract(deductedAmount);
    }

    return new PointDeductionResult(deductedAmount, remainingToPay);
  }

  public Long getAmountValue() {
    return amount.getValue();
  }

}
