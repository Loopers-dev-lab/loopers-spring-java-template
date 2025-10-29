package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointAmount;
import com.loopers.domain.user.User;
import java.util.Objects;

public record PointResult(
    String userId,
    Long amount
) {

  public static PointResult from(Point point) {
    Objects.requireNonNull(point, "Point는 null 일 수 없습니다.");
    User user = point.getUser();
    PointAmount amount = point.getAmount();

    return new PointResult(
        user.getUserId(),
        amount.getAmount()
    );
  }
}
