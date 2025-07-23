package com.loopers.infrastructure.point;

import com.loopers.domain.points.PointsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointJpaRepository extends JpaRepository<PointsEntity, Long> {
}
