package com.loopers.domain.point;

import java.math.BigDecimal;
import java.util.Optional;

public interface PointRepository {
    Optional<Point> findByUser_loginId(String loginId);
    Optional<Point> save(Point point);
    long deduct(String loginId, BigDecimal deductAmount);
    long chargeAmount(String loginId, BigDecimal chargeAmount);
}
