package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "point")
@Getter
public class Point extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Embedded
  private PointAmount amount;

  protected Point() {
  }

  private Point(User user, PointAmount amount) {
    validateUser(user);
    this.user = user;
    this.amount = amount;
  }

  public static Point of(User user, Long amount) {
    return new Point(user, PointAmount.of(amount));
  }

  public static Point of(User user, PointAmount amount) {
    return new Point(user, amount);
  }

  public static Point of(User user) {
    return new Point(user, PointAmount.zero());
  }

  private void validateUser(User user) {
    if (user == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 비어있을 수 없습니다.");
    }
  }

  public void charge(Long chargeAmount) {
    this.amount = this.amount.add(chargeAmount);
  }

}
