package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    Point save(Point point);

    Optional<Point> findByUserId(String userId);

    /**
     * 동시성 제어를 위한 비관적 락을 사용하는 조회 메서드
     *
     * @param userId User ID
     * @return Point
     */
    Optional<Point> findByUserIdWithLock(String userId);

    boolean existsByUserId(String userId);
}
