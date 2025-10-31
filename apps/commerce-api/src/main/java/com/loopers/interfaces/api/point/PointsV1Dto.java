package com.loopers.interfaces.api.point;

import com.loopers.domain.point.Point;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PointsV1Dto {
    public record PointsResponse(String userId, Long balance) {
        public static PointsResponse from(Point point) {
            return new PointsResponse(point.getUser().getUserId(), point.getBalance());
        }
    }

    public record ChargeRequest(
        @NotNull(message = "포인트는 필수입니다.")
        @Positive(message = "포인트는 0보다 큰 값이어야 합니다.")
        Long amount
    ) {}
}