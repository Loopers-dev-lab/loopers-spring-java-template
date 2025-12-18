package com.loopers.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventHandled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxEventProcessor {

  private final InboxService inboxService;
  private final ProductMetricsService productMetricsService;
  private final ObjectMapper objectMapper;

  @Scheduled(fixedDelay = 1000) // 1초마다 실행
  public void processPendingEvents() {
    List<EventHandled> pendingEvents = inboxService.findPendingEvents();

    if (pendingEvents.isEmpty()) {
      return;
    }

    log.info("Processing {} pending events", pendingEvents.size());

    for (EventHandled event : pendingEvents) {
      try {
        // 처리 중 상태로 변경 (동시성 제어)
        inboxService.markAsProcessing(event.getId());

        // 실제 비즈니스 로직 처리
        processEvent(event);

        // 처리 완료
        inboxService.markAsCompleted(event.getId());

      } catch (Exception e) {
        log.error("Failed to process event: businessKey={}, error={}",
            event.getBusinessKey(), e.getMessage(), e);

        if (event.canRetry()) {
          // 재시도 가능하면 PENDING으로 되돌림
          inboxService.markAsFailed(event.getId(), e.getMessage());
        } else {
          // 최대 재시도 횟수 초과시 FAILED로 마킹
          inboxService.markAsFailed(event.getId(),
              "Max retry exceeded: " + e.getMessage());
        }
      }
    }
  }

  private void processEvent(EventHandled event) throws Exception {
    JsonNode eventData = objectMapper.readTree(event.getPayload());

    switch (event.getEventType()) {
      case "OrderCreated" -> processOrderCreated(eventData, event);
      case "OrderCancelled" -> processOrderCancelled(eventData, event);
      case "PaymentCompleted" -> processPaymentCompleted(eventData, event);
      default -> log.warn("Unknown event type: {}", event.getEventType());
    }
  }

  private void processOrderCreated(JsonNode eventData, EventHandled event) {
    Long orderId = eventData.get("orderId").asLong();
    Long userId = eventData.get("userId").asLong();

    log.info("Processing OrderCreated event: orderId={}, userId={}, eventId={}",
        orderId, userId, event.getBusinessKey());

    if (eventData.has("orderItems") && eventData.get("orderItems").isArray()) {
      eventData.get("orderItems").forEach(item -> {
        JsonNode itemNode = (JsonNode) item;
        if (itemNode.isObject()) {
          Long productId = itemNode.get("productId").asLong();
          Long quantity = itemNode.get("quantity").asLong();
          productMetricsService.incrementSalesCount(productId, quantity);
        }
      });
    }

    log.info("OrderCreated metrics updated: orderId={}, eventId={}",
        orderId, event.getBusinessKey());
  }

  private void processOrderCancelled(JsonNode eventData, EventHandled event) {
    log.info("Processing OrderCancelled event: orderId={}, reason={}, eventId={}",
        eventData.get("orderId"), eventData.get("reason"), event.getBusinessKey());
  }

  private void processPaymentCompleted(JsonNode eventData, EventHandled event) {
    log.info("Processing PaymentCompleted event: orderId={}, amount={}, eventId={}",
        eventData.get("orderId"), eventData.get("amount"), event.getBusinessKey());
  }
}
