package com.loopers.infrastructure.ranking;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ranking.redis")
public class RankingRedisProperties {
  private Duration ttl = Duration.ofDays(2);
  private String keyPrefix = "ranking:all";
}
