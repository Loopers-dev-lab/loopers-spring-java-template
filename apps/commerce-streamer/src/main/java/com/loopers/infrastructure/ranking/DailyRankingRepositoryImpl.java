package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.DailyRanking;
import com.loopers.domain.ranking.DailyRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DailyRankingRepositoryImpl implements DailyRankingRepository {

  private final DailyRankingJpaRepository jpaRepository;

  @Override
  public void saveAll(List<DailyRanking> rankings) {
    jpaRepository.saveAll(rankings);
  }

  @Override
  public List<DailyRanking> findByRankingDateOrderByRankingPosition(LocalDate rankingDate) {
    return jpaRepository.findByRankingDateOrderByRankingPosition(rankingDate);
  }

  @Override
  public List<DailyRanking> findByRankingDateAndProductId(LocalDate rankingDate, Long productId) {
    return jpaRepository.findByRankingDateAndProductId(rankingDate, productId);
  }
}