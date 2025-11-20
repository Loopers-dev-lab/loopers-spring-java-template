package com.loopers.infrastructure.point;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import com.loopers.domain.point.Point;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PointJpaRepository extends JpaRepository<Point, Long> {
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Point p WHERE p.userId = :userId")
    Optional<Point> findByUserIdWithLock(@Param("userId") Long userId);

    Optional<Point> findByUserId(Long userId);
}
