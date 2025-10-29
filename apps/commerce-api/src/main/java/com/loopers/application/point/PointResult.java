package com.loopers.application.point;

import com.loopers.domain.point.Point;

public record PointResult(
    String userId,
    Long balance
) {

  public static PointResult from(Point point) {
    return new PointResult(
        point.getUser().getUserId(),
        point.getBalance()
    );
  }
}
