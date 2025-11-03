package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<Point, Long> {

  @Query("SELECT p FROM Point p JOIN FETCH p.user WHERE p.user.userId = :userId")
  Point findByUserId(@Param("userId") String userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Point p JOIN FETCH p.user WHERE p.user.userId = :userId")
  Optional<Point> findByUserIdWithLock(@Param("userId") String userId);

}
