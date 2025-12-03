package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PointFacade {

    private final PointService pointService;

    @Transactional(readOnly = true)
    public PointInfo getPointByUserId(String userId) {
        Point point = pointService.getPointByUserId(userId);
        if (point == null) {
            return null;
        }
        return PointInfo.from(point);
    }

    @Transactional
    public PointInfo charge(String userId, Long amount) {
        Point charged = pointService.charge(userId, amount);
        return PointInfo.from(charged);
    }
}
