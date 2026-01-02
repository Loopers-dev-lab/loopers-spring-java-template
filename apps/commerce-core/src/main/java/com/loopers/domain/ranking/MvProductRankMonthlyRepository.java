package com.loopers.domain.ranking;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface MvProductRankMonthlyRepository {
    List<MvProductRankMonthly> findTop100ByRankingDateOrderByRankingAsc(ZonedDateTime rankingDate);
    Optional<MvProductRankMonthly> findByProductIdAndRankingDate(Long productId, ZonedDateTime rankingDate);
    void deleteByRankingDate(ZonedDateTime rankingDate);
    void saveAll(List<MvProductRankMonthly> rankings);
}
