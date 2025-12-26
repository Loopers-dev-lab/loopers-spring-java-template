package com.loopers.application.event.order;

import com.loopers.domain.common.vo.Money;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCompletedEvent(
    String orderNo,
    Long memberId,
    Money totalPrice,
    List<OrderItemInfo> items,
    LocalDateTime completedAt
) {
    public record OrderItemInfo(
        Long productId,
        int quantity,
        Money price
    ) {}
}
