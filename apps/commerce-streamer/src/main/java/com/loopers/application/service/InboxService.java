package com.loopers.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledRepository;
import com.loopers.domain.event.EventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

  private final EventHandledRepository eventHandledRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void saveEvent(ConsumerRecord<String, String> record) {
    try {
      JsonNode eventData = objectMapper.readTree(record.value());
      String eventType = determineEventType(eventData);
      String businessKey = generateBusinessKey(eventData, eventType);

      // 중복 체크: 이미 존재하면 무시
      if (eventHandledRepository.existsByBusinessKey(businessKey)) {
        log.debug("Event already exists in inbox: {}", businessKey);
        return;
      }

      // Inbox에 이벤트 저장
      EventHandled event = new EventHandled(
          businessKey,
          eventType,
          record.topic(),
          record.value()
      );

      eventHandledRepository.save(event);
      log.info("Event saved to inbox: businessKey={}, topic={}, eventType={}",
          businessKey, record.topic(), eventType);

    } catch (Exception e) {
      log.error("Failed to save event to inbox: topic={}, offset={}",
          record.topic(), record.offset(), e);
      throw new RuntimeException("Failed to save event to inbox", e);
    }
  }

  @Transactional(readOnly = true)
  public List<EventHandled> findPendingEvents() {
    return eventHandledRepository.findByStatus(EventStatus.PENDING);
  }

  @Transactional
  public void markAsProcessing(Long eventId) {
    EventHandled event = eventHandledRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

    event.markAsProcessing();
    eventHandledRepository.save(event);
  }

  @Transactional
  public void markAsCompleted(Long eventId) {
    EventHandled event = eventHandledRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

    event.markAsCompleted();
    eventHandledRepository.save(event);
    log.info("Event marked as completed: {}", eventId);
  }

  @Transactional
  public void markAsFailed(Long eventId, String errorMessage) {
    EventHandled event = eventHandledRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

    event.markAsFailed(errorMessage);
    eventHandledRepository.save(event);
    log.error("Event marked as failed: {}, error: {}", eventId, errorMessage);
  }

  private String generateBusinessKey(JsonNode eventData, String eventType) {
    try {
      return switch (eventType) {
        case "OrderCreated" -> "order_create:" + eventData.get("orderId").asText();
        case "OrderCancelled" -> "order_cancel:" + eventData.get("orderId").asText();
        case "PaymentCompleted" -> "payment_complete:" + eventData.get("orderId").asText();
        case "ProductLiked" -> "product_like:" + eventData.get("productId").asText() + ":" + eventData.get("userId").asText();
        case "ProductUnliked" -> "product_unlike:" + eventData.get("productId").asText() + ":" + eventData.get("userId").asText();
        case "StockReduced" -> "stock_reduce:" + eventData.get("productId").asText();
        case "CouponUsed" -> "coupon_use:" + eventData.get("couponId").asText() + ":" + eventData.get("userId").asText();
        default -> {
          // Unknown 이벤트는 payload 해시로 고유성 보장
          log.warn("Unknown event type: {}, using payload hash", eventType);
          yield "unknown:" + eventType + ":" + eventData.hashCode();
        }
      };
    } catch (Exception e) {
      log.error("Failed to generate business key for eventType: {}", eventType, e);
      throw new RuntimeException("Failed to generate business key", e);
    }
  }

  private String determineEventType(JsonNode eventData) {
    if (eventData.has("eventType")) {
      return eventData.get("eventType").asText();
    }
    return "Unknown";
  }
}
