package com.loopers.interfaces.api.point;

import java.math.BigDecimal;

import com.loopers.domain.user.UserEntity;

public class PointV1Dtos {

    public record PointInfoResponse(
            String username,
            BigDecimal currentPointAmount
    ) {
        public static PointInfoResponse from(PointInfo pointInfo) {
            return new PointInfoResponse(
                    pointInfo.username(),
                    pointInfo.currentPointAmount()
            );
        }
    }

    public record PointInfo(
            String username,
            BigDecimal currentPointAmount
    ) {
        public static PointInfo from(UserEntity user) {
            return new PointInfo(
                    user.getUsername(),
                    user.getPointAmount()
            );
        }
    }

    public record PointChargeRequest(
            BigDecimal amount
    ) {
    }

    public record PointChargeResponse(
            String username,
            BigDecimal totalAmount
    ) {
    }
}
