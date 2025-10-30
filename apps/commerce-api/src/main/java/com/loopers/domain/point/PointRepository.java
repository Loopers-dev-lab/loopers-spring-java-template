package com.loopers.domain.point;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PointRepository {
  PointModel save(PointModel user);

  Optional<PointModel> findByUserId(String userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PointModel> findByUserIdForUpdate(String userId);

}
