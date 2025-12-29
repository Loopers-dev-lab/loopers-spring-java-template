package com.loopers.interfaces.api.ranking;

import com.loopers.domain.ranking.RankingWeight;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ranking/config")
@RequiredArgsConstructor
public class RankingConfigV1Controller implements RankingConfigV1ApiSpec {

    private final RankingWeight rankingWeight;

    @Override
    public RankingConfigV1Dto.WeightConfigResponse getWeights() {
        return RankingConfigV1Dto.WeightConfigResponse.of(
                rankingWeight.getViewWeight(),
                rankingWeight.getLikeWeight(),
                rankingWeight.getOrderWeight()
        );
    }

    @Override
    public RankingConfigV1Dto.WeightConfigResponse updateWeights(RankingConfigV1Dto.WeightConfigRequest request) {
        rankingWeight.updateAllWeights(
                request.viewWeight(),
                request.likeWeight(),
                request.orderWeight()
        );
        return getWeights();
    }

    @Override
    public void resetWeights() {
        rankingWeight.resetToDefault();
    }
}
