package com.loopers.interfaces.api.point;

public class PointV1Dto {

    public record PointResponse(
            Long currentPoint
    ) {
        public static PointV1Dto.PointResponse from(Long currentPoint) {
            return new PointV1Dto.PointResponse(
                    currentPoint
            );
        }
    }

    public record PointChargeRequest(
            int amount
    ) {
    }
}
