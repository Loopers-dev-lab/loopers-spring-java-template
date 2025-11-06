package com.loopers.application.point;

import com.loopers.domain.point.PointModel;

public record PointInfo(Long id, Long userModelId, Long point) {
    public static PointInfo from(final PointModel model) {
        return new PointInfo(
                model.getId(),
                model.getUserModelId(),
                model.getPoint()
        );
    }
}
