package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointInfo;
import lombok.Builder;

import java.math.BigDecimal;

public class PointDto {

    @Builder
    public record PointRequest(
           BigDecimal amount
    ) {}

    @Builder
    public record PointResponse(
            BigDecimal amount
    ) {
        public static PointResponse from(PointInfo pointInfo) {
            return PointResponse.builder()
                    .amount(pointInfo.amount())
                    .build();
        }
    }
}
