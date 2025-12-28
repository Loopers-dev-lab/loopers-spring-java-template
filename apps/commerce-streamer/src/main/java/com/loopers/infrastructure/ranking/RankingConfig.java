package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingScorePolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RankingWeightProperties.class, RankingRedisProperties.class})
public class RankingConfig {

  @Bean
  public RankingScorePolicy rankingScorePolicy(RankingWeightProperties props) {
    return new RankingScorePolicy(props.getView(), props.getLike(), props.getOrder());
  }
}
