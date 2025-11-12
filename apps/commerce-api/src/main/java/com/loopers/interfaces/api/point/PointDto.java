package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointResult;
import jakarta.validation.constraints.Positive;

public class PointDto {

  public record PointResponse(
      Long userId,
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
      @Positive(message = "충전 포인트는 양수여야 합니다.") Long amount
  ) {
  }

  public record ChargeResponse(
      Long userId,
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
