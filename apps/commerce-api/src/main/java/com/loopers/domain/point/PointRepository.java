package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {

  Point save(Point point);

  Optional<Point> findByUserId(Long userId);

  Optional<Point> findByUserIdWithLock(Long userId);

  Optional<Point> findByUserLoginId(String loginId);

  Optional<Point> findByUserLoginIdWithLock(String loginId);
}
