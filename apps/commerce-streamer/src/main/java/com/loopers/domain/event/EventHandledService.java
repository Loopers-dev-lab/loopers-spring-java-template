package com.loopers.domain.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventHandledService {

  private final EventHandledRepository eventHandledRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void saveEvent(ConsumerRecord<String, String> record) {
    try {
      JsonNode eventData = objectMapper.readTree(record.value());
      
      // 필수 필드 존재 여부 검증
      JsonNode eventIdNode = eventData.get("eventId");
      JsonNode eventTypeNode = eventData.get("eventType");
      
      if (eventIdNode == null || eventIdNode.isNull()) {
        log.error("Missing eventId in message: topic={}, offset={}, payload={}", 
                 record.topic(), record.offset(), record.value());
        return; // 필수 필드 없으면 스킵
      }
      
      if (eventTypeNode == null || eventTypeNode.isNull()) {
        log.error("Missing eventType in message: topic={}, offset={}, payload={}", 
                 record.topic(), record.offset(), record.value());
        return; // 필수 필드 없으면 스킵
      }
      
      String eventId = eventIdNode.asText();
      String eventType = eventTypeNode.asText();
      String businessKey = generateBusinessKey(eventData, eventType);

      // Inbox에 이벤트 저장
      EventHandled event = new EventHandled(
          eventId,
          businessKey,
          eventType,
          record.topic(),
          record.value()
      );

      eventHandledRepository.save(event);
      log.info("Event saved to inbox: businessKey={}, eventId={}, topic={}, eventType={}",
          businessKey, eventId, record.topic(), eventType);

    } catch (Exception e) {
      log.error("Failed to save event to inbox: topic={}, offset={}, payload={}",
          record.topic(), record.offset(), record.value(), e);
      throw new RuntimeException("Failed to save event to inbox", e);
    }
  }

  @Transactional(readOnly = true)
  public List<EventHandled> findPendingEvents() {
    return eventHandledRepository.findByStatus(EventStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<EventHandled> findPendingEventsByType(String eventType) {
    return eventHandledRepository.findByStatusAndEventType(EventStatus.PENDING, eventType);
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
        case "OrderCreated" -> "order_create:" + getFieldSafely(eventData, "orderId");
        case "OrderCancelled" -> "order_cancel:" + getFieldSafely(eventData, "orderId");
        case "PaymentCompleted" -> "payment_complete:" + getFieldSafely(eventData, "orderId");
        case "ProductLiked" -> "product_like:" + getFieldSafely(eventData, "productId") + ":" + getFieldSafely(eventData, "userId");
        case "ProductUnliked" -> "product_unlike:" + getFieldSafely(eventData, "productId") + ":" + getFieldSafely(eventData, "userId");
        case "ProductViewed" -> "product_view:" + getFieldSafely(eventData, "productId") + ":" + getFieldSafely(eventData, "userId");
        case "StockReduced" -> "stock_reduce:" + getFieldSafely(eventData, "productId");
        case "CouponUsed" -> "coupon_use:" + getFieldSafely(eventData, "couponId") + ":" + getFieldSafely(eventData, "userId");
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

  private String getFieldSafely(JsonNode eventData, String fieldName) {
    JsonNode field = eventData.get(fieldName);
    if (field == null || field.isNull()) {
      log.warn("Missing field '{}' in event data, using 'unknown'", fieldName);
      return "unknown";
    }
    return field.asText();
  }

}
