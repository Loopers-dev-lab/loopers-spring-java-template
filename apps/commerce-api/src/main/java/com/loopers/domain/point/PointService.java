package com.loopers.domain.point;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;

    public Optional<Point> findByUserId(String userId) {
      return pointRepository.findByUserId(userId);
    }
}
