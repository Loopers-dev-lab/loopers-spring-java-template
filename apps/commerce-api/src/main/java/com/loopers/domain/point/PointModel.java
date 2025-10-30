package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "point")
@Getter
public class PointModel extends BaseEntity {
  private BigDecimal amount;

  @OneToOne
  @JoinColumn(name = "userId", unique = true, nullable = false)
  private UserModel user;

  public void setUser(UserModel user) {
    this.user = user;
    if (user.getPoint() != this) {
      user.setPoint(this);
    }
  }

  protected PointModel() {
    this.amount = BigDecimal.ZERO;
  }

  private PointModel(UserModel user, BigDecimal amount) {
    Objects.requireNonNull(user, "UserModel must not be null for PointModel creation.");

    this.setUser(user);
    this.amount = amount;
  }

  public static PointModel create(UserModel user, BigDecimal amount) {
    return new PointModel(user, amount);
  }

  public PointModel charge(BigDecimal amountToChange) {
    if (amountToChange == null || amountToChange.compareTo(BigDecimal.ZERO) <= 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
    }
    return new PointModel(this.user, this.amount.add(amountToChange));
  }

  public PointModel use(BigDecimal amountToChange) {
    if (amountToChange == null || amountToChange.compareTo(BigDecimal.ZERO) <= 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "차감 금액은 0보다 커야 합니다.");
    }
    if (this.amount.compareTo(amountToChange) < 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "잔액이 부족합니다.");
    }
    return new PointModel(this.user, this.amount.subtract(amountToChange));
  }
}
