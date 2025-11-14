package com.loopers.domain.product;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import lombok.Builder;

@Builder
public record ProductCondition(
    BigDecimal price
    , Long likeCount
    , ZonedDateTime createdAt
    , String sort
) {}
