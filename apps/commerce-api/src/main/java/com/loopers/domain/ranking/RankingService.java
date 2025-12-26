package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

  private final RankingRepository rankingRepository;
  private final RankingKeyPolicy rankingKeyPolicy;

  public List<RankingEntry> getTopN(LocalDate date, int page, int size) {
    String key = rankingKeyPolicy.buildKey(date);
    return rankingRepository.getTopN(key, page, size);
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
}
