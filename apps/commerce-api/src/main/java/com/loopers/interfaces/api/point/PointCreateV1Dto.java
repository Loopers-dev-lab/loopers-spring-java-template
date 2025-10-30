package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;

import java.math.BigDecimal;

public class PointCreateV1Dto {
  public record PointRequest(String userId, BigDecimal amount) {
  }

  public record PointResponse(String userId, BigDecimal amount) {
    public static PointResponse from(PointInfo info) {
      if (info == null) return null;
      return new PointResponse(
          info.userId(),
          info.amount()
      );
    }
  }
}
