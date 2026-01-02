package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingV1Dto;
import com.loopers.domain.ranking.Period;
import com.loopers.interfaces.api.ApiResponse;

public interface RankingV1ApiSpec {

    ApiResponse<RankingV1Dto.ProductRankingPageResponse> getProductRanking(String date, Period period, int size, int page);

}
