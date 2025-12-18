package com.loopers.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.support.test.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("CatalogEventHandler 통합 테스트")
class CatalogEventHandlerIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private CatalogEventHandler catalogEventHandler;

  @Autowired
  private EventHandledRepository eventHandledRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private ObjectNode emptyPayload() {
    return objectMapper.createObjectNode();
  }

  @Nested
  @DisplayName("멱등성 처리")
  class Idempotency {

    @Test
    @DisplayName("새로운 eventId는 처리하고 event_handled에 저장한다")
    void shouldProcessNewEvent() {
      String eventId = "new-event-id-001";
      String eventType = "product_out_of_stock";
      String aggregateId = "123";
      Long occurredAt = System.currentTimeMillis();

      boolean result =
          catalogEventHandler.handle(eventId, eventType, aggregateId, occurredAt, emptyPayload());

      assertThat(result).isTrue();
      assertThat(eventHandledRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    @DisplayName("동일 eventId로 여러 번 요청해도 한 번만 저장된다")
    void shouldBeIdempotent_whenSameEventIdRequestedMultipleTimes() {
      String eventId = "duplicate-event-id-001";
      String eventType = "product_out_of_stock";
      String aggregateId = "456";
      Long occurredAt = System.currentTimeMillis();

      boolean firstResult =
          catalogEventHandler.handle(eventId, eventType, aggregateId, occurredAt, emptyPayload());
      boolean secondResult =
          catalogEventHandler.handle(eventId, eventType, aggregateId, occurredAt, emptyPayload());
      boolean thirdResult =
          catalogEventHandler.handle(eventId, eventType, aggregateId, occurredAt, emptyPayload());

      assertAll(
          () -> assertThat(firstResult).isTrue(),
          () -> assertThat(secondResult).isTrue(),
          () -> assertThat(thirdResult).isTrue());
      assertThat(eventHandledRepository.existsByEventId(eventId)).isTrue();
    }

    @Test
    @DisplayName("서로 다른 eventId는 각각 처리된다")
    void shouldProcessDifferentEventIds() {
      String eventType = "product_out_of_stock";
      String aggregateId = "789";
      Long occurredAt = System.currentTimeMillis();

      catalogEventHandler.handle("event-001", eventType, aggregateId, occurredAt, emptyPayload());
      catalogEventHandler.handle("event-002", eventType, aggregateId, occurredAt, emptyPayload());
      catalogEventHandler.handle("event-003", eventType, aggregateId, occurredAt, emptyPayload());

      assertAll(
          () -> assertThat(eventHandledRepository.existsByEventId("event-001")).isTrue(),
          () -> assertThat(eventHandledRepository.existsByEventId("event-002")).isTrue(),
          () -> assertThat(eventHandledRepository.existsByEventId("event-003")).isTrue());
    }
  }
}
