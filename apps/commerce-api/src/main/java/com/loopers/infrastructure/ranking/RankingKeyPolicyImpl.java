package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingKeyPolicy;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingKeyPolicyImpl implements RankingKeyPolicy {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final RankingRedisProperties redisProperties;

  @Override
  public String buildKey(LocalDate date) {
    return redisProperties.getKeyPrefix() + ":" + date.format(DATE_FORMATTER);
  }
}