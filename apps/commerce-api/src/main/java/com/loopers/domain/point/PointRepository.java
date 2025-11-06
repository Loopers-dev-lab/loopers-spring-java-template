package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    Optional<PointModel> findByUserModelId(Long userModelId);

    PointModel save(PointModel pointModel);
}
