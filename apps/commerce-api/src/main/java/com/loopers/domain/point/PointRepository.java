package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    Optional<Point> findById(String id);
    Point save(Point point);
}
