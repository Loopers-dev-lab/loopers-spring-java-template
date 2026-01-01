package com.loopers.infrastructure.ranking.mv;

import com.loopers.domain.ranking.mv.WeeklyProductRank;
import com.loopers.domain.ranking.mv.WeeklyProductRankRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WeeklyProductRankRepositoryImpl implements WeeklyProductRankRepository {

  private final WeeklyProductRankJpaRepository jpaRepository;

  @Override
  public List<WeeklyProductRank> findByYearWeekOrderByScoreDesc(String yearWeek, int page, int size) {
    return jpaRepository.findByYearWeekOrderByScoreDesc(yearWeek, PageRequest.of(page, size));
  }
}
