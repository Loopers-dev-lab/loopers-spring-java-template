package com.loopers.application.point;

public record PointInfo(String userId, long balance) {
    public static PointInfo from(String userId, long balance) {
        return new PointInfo(userId, balance);
    }
}
