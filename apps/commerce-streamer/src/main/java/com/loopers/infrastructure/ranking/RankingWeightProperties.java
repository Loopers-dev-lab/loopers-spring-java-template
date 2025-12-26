package com.loopers.infrastructure.ranking;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ranking.weight")
public class RankingWeightProperties {
  private double view = 0.1;
  private double like = 0.3;
  private double order = 0.6;
}
