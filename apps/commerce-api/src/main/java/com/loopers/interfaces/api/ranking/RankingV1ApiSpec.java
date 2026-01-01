package com.loopers.interfaces.api.ranking;

import com.loopers.interfaces.api.ApiResponse;

public interface RankingV1ApiSpec {

    ApiResponse<RankingV1Dto.ProductRankingPageResponse> getDailyProductRanking(int size, int page);

}
