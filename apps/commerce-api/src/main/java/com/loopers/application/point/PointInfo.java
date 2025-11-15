package com.loopers.application.point;

import com.loopers.domain.point.Point;

import java.math.BigDecimal;

public record PointInfo(String userId, BigDecimal amount) {
  public static PointInfo from(Point model) {
    if (model == null) return null;
    return new PointInfo(
        model.getUser().getLoginId(),
        model.getAmount()
    );
  }
}
