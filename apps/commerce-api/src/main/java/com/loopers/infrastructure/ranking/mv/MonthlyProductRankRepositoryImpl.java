package com.loopers.infrastructure.ranking.mv;

import com.loopers.domain.ranking.mv.MonthlyProductRank;
import com.loopers.domain.ranking.mv.MonthlyProductRankRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MonthlyProductRankRepositoryImpl implements MonthlyProductRankRepository {

  private final MonthlyProductRankJpaRepository jpaRepository;

  @Override
  public List<MonthlyProductRank> findByYearMonthOrderByScoreDesc(String yearMonth, int page, int size) {
    return jpaRepository.findByYearMonthOrderByScoreDesc(yearMonth, PageRequest.of(page, size));
  }
}
