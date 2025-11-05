package com.loopers.interfaces.api.point;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PointV1Dto {
    public record ChargeRequest(
            @NotNull(message = "충전 금액은 필수입니다.")
            @Positive(message = "충전 금액은 0보다 커야 합니다.")
            Long amount
    ) {
    }

    public record PointResponse(
            String userId,
            Long amount
    ) {
        public static PointResponse of(
                String userId,
                Long amount
        ) {
            return new PointResponse(
                    userId,
                    amount
            );
        }
    }
}
