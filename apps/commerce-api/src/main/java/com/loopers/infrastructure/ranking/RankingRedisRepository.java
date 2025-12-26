package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.RankingRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RankingRedisRepository implements RankingRepository {

  private final StringRedisTemplate redisTemplate;

  @Override
  public List<RankingEntry> getTopN(String key, int page, int size) {
    long start = (long) page * size;
    long end = start + size - 1;

    Set<TypedTuple<String>> tuples =
        redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

    if (tuples == null || tuples.isEmpty()) {
      return List.of();
    }

    List<RankingEntry> entries = new ArrayList<>();
    int rank = (int) start + 1;

    for (TypedTuple<String> tuple : tuples) {
      String productIdStr = tuple.getValue();
      Double score = tuple.getScore();

      if (productIdStr != null && score != null) {
        Long productId = parseProductIdOrNull(productIdStr);
        if (productId != null) {
          entries.add(new RankingEntry(productId, score, rank));
          rank++;
        }
      }
    }

    return entries;
  }

  @Override
  public Integer getRank(String key, Long productId) {
    Long zeroBasedRank = redisTemplate.opsForZSet().reverseRank(key, productId.toString());

    if (zeroBasedRank == null) {
      return null;
    }

    return zeroBasedRank.intValue() + 1;
  }

  private Long parseProductIdOrNull(String productIdStr) {
    try {
      return Long.parseLong(productIdStr);
    } catch (NumberFormatException e) {
      log.warn("잘못된 productId 형식: {}", productIdStr);
      return null;
    }
  }
}
