package com.loopers.application.point;

import com.loopers.domain.point.PointService;
import com.loopers.domain.point.Point;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;

    public Optional<PointResult> findPoint(String loginId) {
      return pointService.findByUserLoginId(loginId)
          .map(PointResult::from);
    }

    public PointResult charge(String loginId, Long amount) {
      return PointResult.from(pointService.charge(loginId, amount));
    }

}
