package com.loopers.application.point;

import com.loopers.domain.point.PointService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;

    public Optional<PointResult> getPoint(String userId) {
      return Optional.ofNullable(pointService.findByUserId(userId))
          .map(PointResult::from);
    }

    public PointResult charge(String userId, Long amount) {
      return PointResult.from(pointService.charge(userId, amount));
    }

}
