package com.loopers.infrastructure.outbox;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox.relay")
public class OutboxProperties {

  private int batchSize = 500;
  private int maxRetry = 2;
  private Duration leaseDuration = Duration.ofSeconds(30);
  private long fixedDelay = 200;
  private List<Duration> retryBackoff = List.of(Duration.ofSeconds(1), Duration.ofSeconds(2));
}