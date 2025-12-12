package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PointV1Dto {
    public record ChargeRequest(
            @NotNull(message = "충전 금액은 필수입니다.")
            @Positive(message = "충전 금액은 0보다 커야 합니다.")
            Long amount
    ) {
        public PointCommand toCommand(String loginId) {
            return new PointCommand(
                    loginId,
                    this.amount
            );
        }
    }

    public record PointResponse(
            String loginId,
            Long amount
    ) {
        public static PointResponse of(
                String loginId,
                Long amount
        ) {
            return new PointResponse(
                    loginId,
                    amount
            );
        }
    }
}
