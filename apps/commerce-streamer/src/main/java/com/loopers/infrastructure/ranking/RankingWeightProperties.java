package com.loopers.infrastructure.ranking;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ranking.weight")
public class RankingWeightProperties {
  @Min(0)
  private double view = 0.1;

  @Min(0)
  private double like = 0.3;

  @Min(0)
  private double order = 0.6;
}
