package com.loopers.application.point;

import com.loopers.domain.point.Point;
import java.util.Objects;

public record PointResult(
    Long userId,
    Long amount
) {

  public static PointResult from(Point point) {
    Objects.requireNonNull(point, "Point는 null 일 수 없습니다.");

    return new PointResult(
        point.getUserId(),
        point.getAmountValue()
    );
  }
}
