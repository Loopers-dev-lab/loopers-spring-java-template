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
    if (page < 0 || size < 1) {
      throw new IllegalArgumentException("page는 0 이상, size는 1 이상이어야 합니다");
    }
    return jpaRepository.findByYearWeekOrderByScoreDesc(yearWeek, PageRequest.of(page, size));
  }
}
