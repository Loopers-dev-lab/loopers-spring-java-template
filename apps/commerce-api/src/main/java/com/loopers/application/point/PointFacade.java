package com.loopers.application.point;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class PointFacade {
    private final PointService pointService;

    public Point getPoint(Long userId) {
        return pointService.getPointOrThrow(userId);
    }

    @Transactional
    public Money chargePoint(Long userId, Money amount) {
        return pointService.chargePoint(userId, amount);
    }
}
