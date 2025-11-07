package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointFacade {
    private final PointService pointService;

    public Point getPoint(Long userId) {
        return pointService.getPointOrThrow(userId);
    }

    public int chargePoint(Long userId, int amount) {
        return pointService.chargePoint(userId, amount);
    }
}
