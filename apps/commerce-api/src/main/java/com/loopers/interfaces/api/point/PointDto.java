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

  public record ChargeRequest(
      Long amount
  ) {
  }

  public record ChargeResponse(
      String userId,
      Long balance
  ) {

    public static ChargeResponse from(PointResult pointResult) {
      return new ChargeResponse(
          pointResult.userId(),
          pointResult.amount()
      );
    }
  }
}
