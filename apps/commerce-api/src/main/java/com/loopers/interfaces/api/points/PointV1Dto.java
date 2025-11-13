package com.loopers.interfaces.api.points;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public class PointV1Dto {

    @Schema(description = "포인트 응답")
    public record PointResponse(
            @Schema(description = "보유 포인트")
            BigDecimal points
    ) {}
}