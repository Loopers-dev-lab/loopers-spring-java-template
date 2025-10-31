package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    Point save(Point point);

    Optional<Point> findByUserId(String userId);
}
