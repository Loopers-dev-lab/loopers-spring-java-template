package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;

public interface DailyRankingRepository {

  void saveAll(List<DailyRanking> rankings);

  List<DailyRanking> findByRankingDateOrderByRankingPosition(LocalDate rankingDate);

  List<DailyRanking> findByRankingDateAndProductId(LocalDate rankingDate, Long productId);
}