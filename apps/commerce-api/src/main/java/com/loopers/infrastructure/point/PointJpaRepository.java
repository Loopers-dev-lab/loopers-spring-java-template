package com.loopers.infrastructure.point;

import com.loopers.domain.points.PointsModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointJpaRepository extends JpaRepository<PointsModel, Long> {
}
