package com.loopers.domain.points;

import java.util.Optional;

public interface PointsRepository {
    PointsModel save(PointsModel pointsModel);
    boolean existsByUserId(Long userId);
    Optional<PointsModel> findByUserId(Long loginId);
    
    /**
     * 동시성 제어를 위한 비관적 잠금으로 포인트 정보 조회
     * 포인트 차감/충전 작업 시 사용
     */
    Optional<PointsModel> findByUserIdForUpdate(Long userId);
    
    void deleteAll();
}

