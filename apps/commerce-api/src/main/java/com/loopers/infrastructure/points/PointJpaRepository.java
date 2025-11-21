package com.loopers.infrastructure.points;

import com.loopers.domain.points.Point;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByMemberId(String memberId);
}