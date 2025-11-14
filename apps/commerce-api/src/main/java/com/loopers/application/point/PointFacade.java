package com.loopers.application.point;

import com.loopers.domain.point.PointService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;

    public Optional<PointResult> getPoint(String loginId) {
      return pointService.findByUserLoginId(loginId)
          .map(PointResult::from);
    }

    @Transactional
    public PointResult charge(String loginId, Long amount) {
      return PointResult.from(pointService.charge(loginId, amount));
    }

}
