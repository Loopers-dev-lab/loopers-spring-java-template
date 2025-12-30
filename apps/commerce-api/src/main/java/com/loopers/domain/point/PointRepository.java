package com.loopers.domain.point;

import java.math.BigDecimal;
import java.util.Optional;

public interface PointRepository {
    Optional<Point> findByUserId(Long userId);
    Optional<Point> save(Point point);
    long deduct(Long userId, BigDecimal deductAmount);
    long chargeAmount(Long userId, BigDecimal chargeAmount);
}
