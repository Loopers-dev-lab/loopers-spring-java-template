package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;

    public PointResult getPoint(String userId) {
        Point point = pointService.findByUserId(userId);
        if (point == null) {
            return null;
        }
        return PointResult.from(point);
    }
}
