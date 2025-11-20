package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    Optional<Point> findByUserIdWithLock(Long userModelId);

    Point save(Point point);
}
