package com.loopers.interfaces.api.ranking;

import jakarta.validation.constraints.Min;

public class RankingConfigV1Dto {

    public record WeightConfigRequest(
            @Min(value = 0, message = "viewWeight는 0 이상이어야 합니다.")
            double viewWeight,

            @Min(value = 0, message = "likeWeight는 0 이상이어야 합니다.")
            double likeWeight,

            @Min(value = 0, message = "orderWeight는 0 이상이어야 합니다.")
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
