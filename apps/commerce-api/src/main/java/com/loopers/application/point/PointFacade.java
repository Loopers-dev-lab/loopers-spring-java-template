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
      return pointService.findByUserId(userId)
          .map(PointResult::from);
    }

}
