package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;

public class PointV1Dto {

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
