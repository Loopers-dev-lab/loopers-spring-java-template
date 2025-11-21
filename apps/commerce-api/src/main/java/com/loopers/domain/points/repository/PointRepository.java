package com.loopers.domain.points.repository;

import com.loopers.domain.points.Point;

import java.util.Optional;

public interface PointRepository {
    Optional<Point> findByMemberId(String memberId);

    Optional<Point> findByMemberIdForUpdate(String memberId);

    Point save(Point point);
}
