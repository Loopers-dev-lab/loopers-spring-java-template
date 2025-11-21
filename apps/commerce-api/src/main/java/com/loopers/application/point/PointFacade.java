package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;

    @Transactional
    public void createPointForUser(String userId) {
        pointService.createPoint(userId);
    }

    @Transactional(readOnly = true)
    public Point getPoint(String userId) {
        return pointService.getPoint(userId);
    }

    @Transactional
    public void chargePoint(PointCommand command) {
        pointService.chargePoint(
                command.userId(),
                command.amount()
        );
    }
}
