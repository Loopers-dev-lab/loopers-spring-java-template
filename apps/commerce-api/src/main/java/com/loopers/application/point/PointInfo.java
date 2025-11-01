package com.loopers.application.point;

import com.loopers.domain.point.Point;

public record PointInfo(Long userId, Long balance) {
    public static PointInfo from(Point point) {
        return new PointInfo(
            point.getUserId(),
            point.getBalance()
        );
    }
}
