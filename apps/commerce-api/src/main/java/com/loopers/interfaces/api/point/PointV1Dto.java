package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;

public class PointV1Dto {

    public record PointBalanceResponse(String userId, long balance) {
        public static PointBalanceResponse from(PointInfo info) {
            return new PointBalanceResponse(
                    info.userId(),
                    info.balance()
            );
        }
    }
}
