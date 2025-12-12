package com.loopers.fixture;

import com.loopers.domain.point.Point;

public class PointFixture {

    // 기본 포인트 (0원)
    public static Point empty(Long userId) {
        return Point.create(userId);
    }

    // 포인트 금액 지정
    public static Point withBalance(Long userId, Long balance) {
        return Point.create(userId, balance);
    }

    // 소액 포인트
    public static Point small(Long userId) {
        return Point.create(userId, 1000L);
    }

    // 중간 포인트
    public static Point medium(Long userId) {
        return Point.create(userId, 50000L);
    }

    // 대량 포인트
    public static Point large(Long userId) {
        return Point.create(userId, 1000000L);
    }

    // 테스트용 충분한 포인트 (10만원)
    public static Point sufficient(Long userId) {
        return Point.create(userId, 100000L);
    }
}
