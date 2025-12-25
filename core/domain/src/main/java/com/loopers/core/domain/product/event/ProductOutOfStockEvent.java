package com.loopers.core.domain.product.event;

import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.product.vo.ProductId;

public record ProductOutOfStockEvent(
        EventId eventId,
        ProductId productId
) {
}
