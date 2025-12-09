package com.loopers.core.service.order.event;

import com.loopers.core.domain.order.vo.OrderId;

public record OrderCompletedEvent(OrderId orderId) {
}
