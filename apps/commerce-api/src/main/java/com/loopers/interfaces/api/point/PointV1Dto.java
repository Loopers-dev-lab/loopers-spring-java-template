package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PointV1Dto {

    @NotNull(message = "충전 금액은 필수입니다.")
    @Positive(message = "충전 금액은 양수여야 합니다.")
    public record ChargeRequest(
            Long amount
    ) {
    }

    public record PointResponse(
            Long amount
    ) {
        public static PointResponse from(PointInfo info) {
            return new PointResponse(
                    info.amount()
            );
        }
    }
}
