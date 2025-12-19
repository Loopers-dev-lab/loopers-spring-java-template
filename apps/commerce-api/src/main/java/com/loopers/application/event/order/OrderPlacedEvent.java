package com.loopers.application.event.order;

import com.loopers.domain.common.vo.Money;

import java.time.LocalDateTime;

public record OrderPlacedEvent(
    String orderNo,
    Long memberId,
    Long memberCouponId,  // nullable
    Money totalPrice,
    LocalDateTime placedAt
) {
    public boolean hasCoupon() {
        return memberCouponId != null;
    }
}
