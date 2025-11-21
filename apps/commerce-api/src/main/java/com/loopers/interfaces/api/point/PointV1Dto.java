package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;
import com.loopers.domain.common.Money;

public class PointV1Dto {
    public record PointResponse(String userId, Money point) {
        public static PointResponse from(PointInfo info) {
            return new PointResponse(
                info.user().getUserId().userId(),
                info.point()
            );
        }
    }

    public record ChargeRequest(
        Money point
    ) {}
}
