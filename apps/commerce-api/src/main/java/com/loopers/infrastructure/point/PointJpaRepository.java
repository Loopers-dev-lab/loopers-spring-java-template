package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<Point, Integer> {
    Optional<Point> findByUser_loginId(String loginId);
}
