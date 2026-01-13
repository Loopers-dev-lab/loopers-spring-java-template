package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;
import jakarta.validation.constraints.Min;

public class PointV1Dto {
    public record PointResponse(String userId, int point) {
        public static PointResponse from(PointInfo info) {
            return new PointResponse(
                info.user().getUserId(),
                info.point()
            );
        }
    }

    public record ChargeRequest(
        @Min(value = 1, message = "포인트는 1 이상이어야 합니다.")
        int amount
    ) {}
}
