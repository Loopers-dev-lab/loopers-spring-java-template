package com.loopers.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OutboxEvent(
        Long id,
        String aggregateType,
        Long aggregateId,
        String eventType,
        String status,
        BigDecimal createdAt,
        BigDecimal updatedAt
) {
}
