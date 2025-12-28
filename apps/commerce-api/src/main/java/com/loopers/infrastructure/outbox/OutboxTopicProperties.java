package com.loopers.infrastructure.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox.topics")
public class OutboxTopicProperties {

  private String envPrefix = "";
  private String catalogEvents = "catalog-events";
  private String orderEvents = "order-events";

  public String getCatalogEventsTopic() {
    return buildTopicName(catalogEvents);
  }

  public String getOrderEventsTopic() {
    return buildTopicName(orderEvents);
  }

  private String buildTopicName(String topic) {
    return envPrefix.isEmpty() ? topic : envPrefix + "-" + topic;
  }
}
