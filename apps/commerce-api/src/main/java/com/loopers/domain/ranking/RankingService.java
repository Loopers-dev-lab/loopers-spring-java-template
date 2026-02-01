package com.loopers.domain.ranking;

import com.loopers.domain.ranking.mv.MonthlyProductRank;
import com.loopers.domain.ranking.mv.MonthlyProductRankRepository;
import com.loopers.domain.ranking.mv.WeeklyProductRank;
import com.loopers.domain.ranking.mv.WeeklyProductRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

  private final RankingRepository rankingRepository;
  private final RankingKeyPolicy rankingKeyPolicy;
  private final WeeklyProductRankRepository weeklyProductRankRepository;
  private final MonthlyProductRankRepository monthlyProductRankRepository;

  public List<RankingEntry> getTopN(LocalDate date, int page, int size) {
    String key = rankingKeyPolicy.buildKey(date);
    return rankingRepository.getTopN(key, page, size);
  }

  public List<RankingEntry> getWeeklyTopN(LocalDate date, int page, int size) {
    String yearWeek = toYearWeek(date);
    List<WeeklyProductRank> ranks = weeklyProductRankRepository.findByYearWeekOrderByScoreDesc(yearWeek, page, size);
    return toRankingEntries(ranks, page, size);
  }

  public List<RankingEntry> getMonthlyTopN(LocalDate date, int page, int size) {
    String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    List<MonthlyProductRank> ranks = monthlyProductRankRepository.findByYearMonthOrderByScoreDesc(yearMonth, page, size);
    return toRankingEntries(ranks, page, size);
  }

  private List<RankingEntry> toRankingEntries(List<? extends ProductRankView> ranks, int page, int size) {
    int baseRank = page * size;
    List<RankingEntry> result = new ArrayList<>();
    for (int i = 0; i < ranks.size(); i++) {
      ProductRankView rank = ranks.get(i);
      result.add(new RankingEntry(rank.getRefProductId(), rank.getScore(), baseRank + i + 1));
    }
    return result;
  }

  public Integer getRankOrNull(LocalDate date, Long productId) {
    try {
      String key = rankingKeyPolicy.buildKey(date);
      return rankingRepository.getRank(key, productId);
    } catch (RedisConnectionFailureException | RedisSystemException e) {
      log.warn("랭킹 조회 실패: productId={}, date={}", productId, date, e);
      return null;
    }
  }

  private String toYearWeek(LocalDate date) {
    WeekFields weekFields = WeekFields.of(Locale.getDefault());
    int weekBasedYear = date.get(weekFields.weekBasedYear());
    int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
    return String.format("%d-W%02d", weekBasedYear, weekOfYear);
  }
}
