package com.loopers.domain.product;

import com.loopers.domain.common.vo.Money;

import java.time.ZonedDateTime;

public interface ProductListProjection {
    Long getId();
    String getName();
    Money getPrice();
    Long getBrandId();
    int getLikeCount();
    ZonedDateTime getCreatedAt();
}
