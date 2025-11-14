package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<Point, Long> {
  boolean existsByUserUserId(String userId);

  Optional<Point> findByUserUserId(String userId);

  Optional<Point> findByUserId(Long userId);

  @Query("SELECT p FROM Point p WHERE p.user.id = :userId")
  Optional<Point> findByUserIdForUpdate(@Param("userId") Long userId);


}
