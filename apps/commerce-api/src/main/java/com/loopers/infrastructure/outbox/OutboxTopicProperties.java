package com.loopers.infrastructure.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox.topics")
public class OutboxTopicProperties {

  private String catalogEvents = "catalog-events";
  private String orderEvents = "order-events";
}
