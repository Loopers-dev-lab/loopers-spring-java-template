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

  public void deduct(Long deductAmount) {
    this.amount = this.amount.subtract(deductAmount);
  }

  public boolean hasEnoughBalance(Long requiredAmount) {
    if (requiredAmount == null || requiredAmount < 0) {
      return false;
    }
    return this.amount.getValue() >= requiredAmount;
  }

  public boolean isNotEnough(Long requiredAmount) {
    return !hasEnoughBalance(requiredAmount);
  }

  public Long getAmountValue() {
    return amount.getValue();
  }

}
