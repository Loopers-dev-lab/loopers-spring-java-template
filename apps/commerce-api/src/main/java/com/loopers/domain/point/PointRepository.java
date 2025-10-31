package com.loopers.domain.point;

public interface PointRepository {
    Point save(Point point);
    Point findByUserId(String userId);
}


