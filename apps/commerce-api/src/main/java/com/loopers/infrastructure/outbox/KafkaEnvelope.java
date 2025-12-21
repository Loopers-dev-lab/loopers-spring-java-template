package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.annotation.JsonProperty;

// Kafka 메시지 공통 봉투 (메타데이터 + payload)
public record KafkaEnvelope(
    @JsonProperty("schemaVersion") int schemaVersion,
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("occurredAt") long occurredAt,
    @JsonProperty("payload") Object payload) {

  public static KafkaEnvelope of(
      String eventId, String eventType, String aggregateId, long occurredAt, Object payload) {
    return new KafkaEnvelope(1, eventId, eventType, aggregateId, occurredAt, payload);
  }
}
