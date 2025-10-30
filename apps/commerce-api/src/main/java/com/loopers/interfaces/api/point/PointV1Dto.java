package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;

public class PointV1Dto {

    public record PointResponse(String userId, long balance) {
        public static PointResponse from(PointInfo info) {
            return new PointResponse(
                    info.userId(),
                    info.balance()
            );
        }
    }

    public record PointChargeRequest(long amount) {}

}
