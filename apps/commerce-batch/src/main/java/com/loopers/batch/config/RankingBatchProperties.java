package com.loopers.batch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ranking")
public class RankingBatchProperties {

  private Weight weight = new Weight();
  private Batch batch = new Batch();

  @Getter
  @Setter
  public static class Weight {
    private double view = 0.1;
    private double like = 0.3;
    private double order = 0.6;
  }

  @Getter
  @Setter
  public static class Batch {
    private int chunkSize = 100;
    private int limit = 100;
  }
}
