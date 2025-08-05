package com.loopers.application.points;

import com.loopers.domain.points.PointsModel;

import java.math.BigDecimal;


public class PointsCommand {
    public record PointInfo(
            Long loginId,
            BigDecimal amount){
        public static PointInfo from(PointsModel pointsModel) {
            return new PointInfo(
                    pointsModel.getId(),
                    pointsModel.getPoint()
            );
        }
    }
}
