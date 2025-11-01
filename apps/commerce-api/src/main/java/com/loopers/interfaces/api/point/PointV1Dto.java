package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;

public class PointV1Dto {
    public record PointResponse(Long userId, Long balance) {
        public static PointResponse from(PointInfo pointInfo) {
            return new PointResponse(
                pointInfo.userId(),
                pointInfo.balance()
            );
        }
    }

    public record PointChargeRequest(Long userId, Long amount) {
        public PointChargeRequest toEntity() {
            return new PointChargeRequest(
                userId,
                amount
            );
        }
    }
}
