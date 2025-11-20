package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointJpaRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByUserModelId(Long userModelId);
}
