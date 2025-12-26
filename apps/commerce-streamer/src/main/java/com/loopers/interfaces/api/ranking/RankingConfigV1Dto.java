package com.loopers.interfaces.api.ranking;

public class RankingConfigV1Dto {

    public record WeightConfigRequest(
            double viewWeight,
            double likeWeight,
            double orderWeight
    ) {}

    public record WeightConfigResponse(
            double viewWeight,
            double likeWeight,
            double orderWeight
    ) {
        public static WeightConfigResponse of(double viewWeight, double likeWeight, double orderWeight) {
            return new WeightConfigResponse(viewWeight, likeWeight, orderWeight);
        }
    }
}
