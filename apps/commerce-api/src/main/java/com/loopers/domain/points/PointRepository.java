package com.loopers.domain.points;

import java.util.Optional;

public interface PointRepository {
    Optional<PointModel> findByMemberId(String memberId);
    PointModel save(PointModel pointModel);
}
