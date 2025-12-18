package com.loopers.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

  private final KafkaTemplate<Object, Object> kafkaTemplate;
  private final OutboxService outboxService;
  private final ObjectMapper objectMapper;

  public void publishEvent(OutboxEvent outboxEvent) {
    String topic = getTopicByEventType(outboxEvent.getEventType());

    CompletableFuture<SendResult<Object, Object>> future = kafkaTemplate.send(
        topic,
        outboxEvent.getAggregateId(), // partition key
        outboxEvent.getPayload()
    );

    future.whenComplete((result, ex) -> {
      if (ex != null) {
        log.error("Failed to send event to Kafka: {}", outboxEvent.getId(), ex);
        outboxService.markAsFailed(outboxEvent.getId(), ex.getMessage());
      } else {
        log.info("Successfully sent event to Kafka: {} to topic: {}", outboxEvent.getId(), topic);
        outboxService.markAsProcessed(outboxEvent.getId());
      }
    });
  }

  private String getTopicByEventType(String eventType) {
    return switch (eventType) {
      case "OrderCreated", "OrderCancelled", "PaymentCompleted" -> "order-events";
      case "StockReduced", "ProductLiked", "ProductUnliked", "ProductViewed" -> "catalog-events";
      case "CouponUsed" -> "coupon-events";
      default -> "commerce.events.default";
    };
  }
}
