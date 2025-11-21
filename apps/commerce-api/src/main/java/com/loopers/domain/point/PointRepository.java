package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
  Optional<Point> findByUserId(Long userId);

  Optional<Point> findByUserLoginId(String userId);

  Optional<Point> findByUserIdForUpdate(Long userId);

  Point save(Point point);

}
