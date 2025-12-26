package com.loopers.domain.ranking;

import com.loopers.infrastructure.ranking.RankingRedisProperties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final RankingRepository rankingRepository;
  private final RankingRedisProperties redisProperties;

  public List<RankingEntry> getTopN(LocalDate date, int page, int size) {
    String key = buildKey(date);
    return rankingRepository.getTopN(key, page, size);
  }

  public Integer getRankOrNull(LocalDate date, Long productId) {
    try {
      String key = buildKey(date);
      return rankingRepository.getRank(key, productId);
    } catch (Exception e) {
      log.warn("랭킹 조회 실패: productId={}, date={}", productId, date, e);
      return null;
    }
  }

  private String buildKey(LocalDate date) {
    return redisProperties.getKeyPrefix() + ":" + date.format(DATE_FORMATTER);
  }
}
