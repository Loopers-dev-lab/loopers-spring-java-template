package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    Optional<Point> findPointByUserId(Long id);

    Point savePoint(Point newPoint);
}
