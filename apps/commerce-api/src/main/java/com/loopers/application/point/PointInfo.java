package com.loopers.application.point;

import com.loopers.domain.point.PointModel;

import java.math.BigDecimal;

public record PointInfo(String userId, BigDecimal amount) {
  public static PointInfo from(PointModel model) {
    if (model == null) return null;
    return new PointInfo(
        model.getUser().getUserId(),
        model.getAmount()
    );
  }
}
