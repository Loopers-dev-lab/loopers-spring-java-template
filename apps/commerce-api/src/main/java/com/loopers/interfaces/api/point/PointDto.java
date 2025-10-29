package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointResult;

public class PointDto {

  public record PointResponse(
      String userId,
      Long balance
  ) {

    public static PointResponse from(PointResult pointResult) {
      return new PointResponse(
          pointResult.userId(),
          pointResult.amount()
      );
    }
  }
}
