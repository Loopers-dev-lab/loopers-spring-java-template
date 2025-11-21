package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    Optional<Point> findByUserId(Long userId);

    void save(Point point);
}
