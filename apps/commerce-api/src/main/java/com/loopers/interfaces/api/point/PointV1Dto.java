package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;

public class PointV1Dto {

    public record ChargePointRequest(String userId, Long chargeAmount) {
    }

    public record PointResponse(String userId, Long amount) {
        public static PointResponse from(PointInfo info) {
            return new PointResponse(
                    info.userId(),
                    info.amount()
            );
        }
    }
}
