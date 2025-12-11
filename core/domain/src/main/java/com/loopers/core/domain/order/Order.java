package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.type.OrderStatus;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.user.vo.UserId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Order {

    private final OrderId id;

    private final UserId userId;

    private final OrderKey orderKey;

    private final OrderStatus status;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private Order(
            OrderId id,
            UserId userId,
            OrderKey orderKey,
            OrderStatus status,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.orderKey = orderKey;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Order mappedBy(
            OrderId id,
            UserId userId,
            OrderKey orderKey,
            OrderStatus status,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new Order(id, userId, orderKey, status, createdAt, updatedAt, deletedAt);
    }

    public static Order create(
            UserId userId
    ) {
        return new Order(
                OrderId.empty(),
                userId,
                OrderKey.generate(),
                OrderStatus.WAITING_PAYMENT,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }

    public Order payed() {
        return this.toBuilder()
                .status(OrderStatus.PAYED)
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
