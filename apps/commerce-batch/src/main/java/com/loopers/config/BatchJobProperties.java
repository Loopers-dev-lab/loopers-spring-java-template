package com.loopers.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "batch")
public class BatchJobProperties {

  /**
   * 실행할 배치 잡 이름
   * dailyRankingDataProcessingJob | weeklyRankingMVUpdateJob | monthlyRankingMVUpdateJob
   */
  private String jobName;
}
