package com.loopers.support.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record EventEnvelop (
    UUID eventId,
    String eventType,
    Long aggregateId,
    JsonNode payload
) {}
