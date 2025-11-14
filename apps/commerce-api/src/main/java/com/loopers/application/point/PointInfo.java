package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.interfaces.api.point.PointDto;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PointInfo(
        BigDecimal amount
) {
    public static PointInfo from(Point point) {
        return PointInfo.builder()
                .amount(point.getAmount())
                .build();
    }

    public static PointInfo from(PointDto.PointRequest pointRequest) {
        return PointInfo.builder()
                .amount(pointRequest.amount())
                .build();
    }
}
