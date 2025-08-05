package com.loopers.domain.points;

import java.util.Optional;

public interface PointsRepository {
    PointsModel save(PointsModel pointsModel);
    boolean existsByUserId(Long userId);
    Optional<PointsModel> findByUserId(Long loginId);
}

