package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.user.vo.UserId;
import lombok.Getter;

@Getter
public class OrderListItem {

    private final OrderId orderId;

    private final UserId userId;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    public OrderListItem(
            OrderId orderId,
            UserId userId,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
