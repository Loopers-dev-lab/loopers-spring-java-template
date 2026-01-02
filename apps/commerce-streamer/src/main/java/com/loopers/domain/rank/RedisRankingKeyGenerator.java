package com.loopers.domain.rank;

import org.springframework.stereotype.Component;

@Component
public class RedisRankingKeyGenerator implements RankingKeyGenerator {

  private static final String DAILY_RANKING_PREFIX = "ranking:all:";

  @Override
  public String generateDailyKey(String date) {
    return DAILY_RANKING_PREFIX + date;
  }
}
