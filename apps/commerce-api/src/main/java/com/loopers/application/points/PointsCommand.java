package com.loopers.application.points;

import com.loopers.domain.points.PointsEntity;

import java.math.BigDecimal;


public class PointsCommand {
    public record PointInfo(
            Long loginId,
            BigDecimal amount){
        public static PointInfo from(PointsEntity pointsModel) {
            return new PointInfo(
                    pointsModel.getId(),
                    pointsModel.getPoint()
            );
        }
    }
}
