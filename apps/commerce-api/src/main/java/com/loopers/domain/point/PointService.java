package com.loopers.domain.point;

import java.util.Optional;

public interface PointService {
    Optional<Integer> findPoint(Long userId);

    Point getPointOrThrow(Long userId);

    int chargePoint(Long userId, int amount);
}
