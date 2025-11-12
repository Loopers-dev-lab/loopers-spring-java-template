package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;
import com.loopers.domain.point.Point;

public class PointV1Dto {
    public record PointResponse(String userId, Point point) {
        public static PointResponse from(PointInfo info) {
            return new PointResponse(
                info.user().getUserId().userId(),
                info.getPoint()
            );
        }
    }

    public record ChargeRequest(
        Point point
    ) {}
}
