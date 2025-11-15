package com.loopers.domain.point;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PointRepository {
  Optional<Point> findByUserId(Long userId);

  Optional<Point> findByUserLoginId(String userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Point> findByUserIdForUpdate(Long userId);

  Point save(Point point);

}
