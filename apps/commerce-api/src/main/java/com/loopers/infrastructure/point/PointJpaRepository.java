package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointJpaRepository extends JpaRepository<PointModel, Long> {
    Optional<PointModel> findByUserModelId(Long userModelId);
}
