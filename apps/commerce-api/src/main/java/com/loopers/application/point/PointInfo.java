package com.loopers.application.point;

import com.loopers.domain.point.Point;

public record PointInfo(
        String userId,
        Long amount
) {
    /**
     * Create a PointInfo DTO from a domain Point.
     *
     * @param point the domain Point whose userId and amount are copied into the DTO
     * @return a PointInfo containing the userId and amount from the given point
     */
    public static PointInfo from(Point point) {
        return new PointInfo(
                point.getUserId(),
                point.getAmount()
        );
    }
}