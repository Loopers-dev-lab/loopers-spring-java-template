package com.loopers.domain.dataplatform;

import java.time.ZonedDateTime;

public record OrderData(
        String orderKey,
        Long userId,
        String loginId,
        Integer originalTotalPrice,
        Integer discountPrice,
        Integer finalAmount,
        String orderStatus,
        Long couponId,
        ZonedDateTime createdAt
) {
}

