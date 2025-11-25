package com.loopers.domain.product;

import java.time.ZonedDateTime;

import lombok.Builder;

@Builder
public record ProductCondition(
    Long brandId
    , ZonedDateTime createdAt
    , String sort
) {}
