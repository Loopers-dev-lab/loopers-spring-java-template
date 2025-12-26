package com.loopers.domain.ranking;

import java.time.Duration;
import java.util.Map;

public interface RankingRepository {

  void incrementScores(Map<String, Map<Long, Double>> bucketToScores);

  void setTtlIfAbsent(String key, Duration ttl);
}
