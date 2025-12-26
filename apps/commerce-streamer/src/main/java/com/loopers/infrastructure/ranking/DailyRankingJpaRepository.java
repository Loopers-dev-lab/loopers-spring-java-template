package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.DailyRanking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyRankingJpaRepository extends JpaRepository<DailyRanking, Long> {

  List<DailyRanking> findByRankingDateOrderByRankingPosition(LocalDate rankingDate);

  List<DailyRanking> findByRankingDateAndProductId(LocalDate rankingDate, Long productId);
}