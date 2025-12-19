package com.loopers.application.event.order;

import com.loopers.domain.common.vo.Money;
import java.time.LocalDateTime;

public record OrderCompletedEvent(
    String orderNo,
    Long memberId,
    Money totalPrice,
    LocalDateTime completedAt
) {
}
