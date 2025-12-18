package com.loopers.domain.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loopers.support.generator.ULIDGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;
  private final ULIDGenerator ulidGenerator;

  @Transactional
  public void saveEvent(String aggregateType, String aggregateId, String eventType, Object eventData) {
    try {
      String eventId = ulidGenerator.generate();

      // payload에 eventId 추가
      ObjectNode eventWithId = objectMapper.valueToTree(eventData);
      eventWithId.put("eventId", eventId);

      String payload = objectMapper.writeValueAsString(eventWithId);
      OutboxEvent outboxEvent = new OutboxEvent(eventId, aggregateType, aggregateId, eventType, payload);
      outboxEventRepository.save(outboxEvent);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize event data", e);
    }
  }

  @Transactional
  public void markAsProcessed(Long id) {
    OutboxEvent outboxEvent = outboxEventRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("OutboxEvent not found: " + id));
    outboxEvent.markAsProcessed();
  }

  @Transactional
  public void markAsFailed(Long id, String errorMessage) {
    OutboxEvent outboxEvent = outboxEventRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("OutboxEvent not found: " + id));
    outboxEvent.markAsFailed(errorMessage);
  }
}
