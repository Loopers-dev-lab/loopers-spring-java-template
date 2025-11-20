package com.loopers.application.point;

import com.loopers.domain.point.Point;

public record PointInfo(Long id, Long userModelId, Long point) {
    public static PointInfo from(final Point model) {
        return new PointInfo(
                model.getId(),
                model.getUserId(),
                model.getPoint()
        );
    }
}
