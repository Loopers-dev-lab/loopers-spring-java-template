package com.loopers.domain.ranking.mv;

import java.util.List;

public interface WeeklyProductRankRepository {

  List<WeeklyProductRank> findByYearWeekOrderByScoreDesc(String yearWeek, int page, int size);
}
