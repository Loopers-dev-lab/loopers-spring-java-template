package com.loopers.domain.ranking.mv;

import java.util.List;

public interface MonthlyProductRankRepository {

  List<MonthlyProductRank> findByYearMonthOrderByScoreDesc(String yearMonth, int page, int size);
}
