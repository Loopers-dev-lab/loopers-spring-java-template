package com.loopers.application.point;

import com.loopers.domain.point.Point;

public record PointInfo(Integer totalPoint) {
    public static PointInfo from(Point point) {
        return new PointInfo(
                point.getAmount()
        );
    }
}
