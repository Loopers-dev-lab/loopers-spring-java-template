package com.loopers.application.point;

public record PointCommand(
        String userBusinessId,
        Long amount
) {
}
