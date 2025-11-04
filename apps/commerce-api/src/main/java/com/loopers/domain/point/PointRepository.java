package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    Optional<Point> findByUserId(Long id);

    Point save(Point newPoint);
}
