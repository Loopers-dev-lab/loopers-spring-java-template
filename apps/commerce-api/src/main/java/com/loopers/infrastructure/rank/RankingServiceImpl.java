package com.loopers.infrastructure.rank;

import com.loopers.application.rank.RankingInfo;
import com.loopers.domain.rank.monthly.MonthlyRankingMVRepository;
import com.loopers.domain.rank.weekly.WeeklyRankingMVRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RankingServiceImpl implements RankingService {

  private final RedisTemplate<String, String> redisTemplate;
  private final WeeklyRankingMVRepository weeklyRepository;
  private final MonthlyRankingMVRepository monthlyRepository;

  @Override
  public List<Long> getTopRankingIds(String date, int page, int size) {
    String key = "ranking:all:" + date;
    int start = (page - 1) * size;
    int end = start + size - 1;

    Set<String> rankedIds = redisTemplate.opsForZSet().reverseRange(key, start, end);

    if (rankedIds == null || rankedIds.isEmpty()) {
      return List.of();
    }

    return rankedIds.stream()
        .map(Long::valueOf)
        .toList();
  }

  @Override
  public List<RankingInfo> getWeeklyRankings(String date, int page, int size) {
    return weeklyRepository.findByBaseDateOrderByCurrentRankAsc(date, PageRequest.of(page - 1, size))
        .stream()
        .map(RankingInfo::from)
        .toList();
  }

  @Override
  public List<RankingInfo> getMonthlyRankings(String date, int page, int size) {
    return monthlyRepository.findByBaseDateOrderByCurrentRankAsc(date, PageRequest.of(page - 1, size))
        .stream()
        .map(RankingInfo::from)
        .toList();
  }

  @Override
  public Integer getProductRank(Long productId) {
    String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String key = "ranking:all:" + today;

    Long rank = redisTemplate.opsForZSet().reverseRank(key, String.valueOf(productId));

    return (rank != null) ? rank.intValue() + 1 : null;
  }
}
