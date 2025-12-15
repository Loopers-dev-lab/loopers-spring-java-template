package com.loopers.application.point;

public record PointCommand(
        String loginId,
        Long amount
) {
}
