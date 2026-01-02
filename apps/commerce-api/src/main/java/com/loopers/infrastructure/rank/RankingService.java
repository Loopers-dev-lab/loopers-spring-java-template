package com.loopers.infrastructure.rank;

import com.loopers.application.rank.RankingInfo;
import java.util.List;

public interface RankingService {

  List<Long> getTopRankingIds(String date, int page, int size);

  List<RankingInfo> getWeeklyRankings(String date, int page, int size);

  List<RankingInfo> getMonthlyRankings(String date, int page, int size);

  Integer getProductRank(Long productId);
}
