package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "point")
@Getter
public class Point extends BaseEntity {
  private BigDecimal amount;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ref_user_Id", unique = true, nullable = false)
  private User user;

  protected Point() {
    this.amount = BigDecimal.ZERO;
  }

  private Point(User user, BigDecimal amount) {
    Objects.requireNonNull(user, "유저 정보가 없습니다.");

    this.user = user;
    this.amount = amount;
  }

  public static Point create(User user, BigDecimal amount) {
    return new Point(user, amount);
  }

  public void charge(BigDecimal amountToChange) {
    if (amountToChange == null || amountToChange.compareTo(BigDecimal.ZERO) <= 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
    }
    this.amount = this.amount.add(amountToChange);
  }

  public void use(BigDecimal amountToChange) {
    if (amountToChange == null || amountToChange.compareTo(BigDecimal.ZERO) <= 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "차감 금액은 0보다 커야 합니다.");
    }
    if (this.amount.compareTo(amountToChange) < 0) {
      throw new CoreException(ErrorType.INSUFFICIENT_POINT, "포인트가 부족합니다.");
    }
    this.amount = this.amount.subtract(amountToChange);
  }
}
