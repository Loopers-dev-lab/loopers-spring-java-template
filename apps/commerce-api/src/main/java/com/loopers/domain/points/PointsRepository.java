package com.loopers.domain.points;

import java.util.Optional;

public interface PointsRepository {
    PointsEntity save(PointsEntity pointsModel);
    boolean existsByUserId(Long userId);
    Optional<PointsEntity> findByUserId(Long loginId);
}

