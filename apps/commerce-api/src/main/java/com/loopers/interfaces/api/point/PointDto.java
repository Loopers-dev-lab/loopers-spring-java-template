package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointResult;
import jakarta.validation.constraints.Positive;

import java.util.Objects;

public class PointDto {

  private PointDto() {
  }

  public record PointResponse(
      Long userId,
      Long balance
  ) {

    public static PointResponse from(PointResult pointResult) {
      Objects.requireNonNull(pointResult, "pointResult는 null일 수 없습니다.");
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
      Objects.requireNonNull(pointResult, "pointResult는 null일 수 없습니다.");
      return new ChargeResponse(
          pointResult.userId(),
          pointResult.amount()
      );
    }
  }
}
