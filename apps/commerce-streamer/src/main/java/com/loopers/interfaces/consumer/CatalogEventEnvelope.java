package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record CatalogEventEnvelope(
    @JsonProperty("schemaVersion") Integer schemaVersion,
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("aggregateId") String aggregateId,
    @JsonProperty("occurredAt") Long occurredAt,
    @JsonProperty("payload") JsonNode payload) {

}
