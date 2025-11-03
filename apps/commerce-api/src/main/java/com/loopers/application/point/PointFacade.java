package com.loopers.application.point;

import com.loopers.domain.point.PointService;
import com.loopers.domain.point.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;

    public PointResult findPointOrNull(String userId) {
      Point point = pointService.findByUserId(userId);
      return point == null ? null : PointResult.from(point);
    }

    public PointResult charge(String userId, Long amount) {
      return PointResult.from(pointService.charge(userId, amount));
    }

}
