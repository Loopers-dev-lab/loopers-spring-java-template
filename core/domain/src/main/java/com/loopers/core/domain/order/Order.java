package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.user.vo.UserId;
import lombok.Getter;

@Getter
public class Order {

    private final OrderId id;

    private final UserId userId;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    private Order(
            OrderId id,
            UserId userId,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Order mappedBy(
            OrderId id,
            UserId userId,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new Order(id, userId, createdAt, updatedAt, deletedAt);
    }


    public static Order create(
            UserId userId
    ) {
        return new Order(
                OrderId.empty(),
                userId,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }
}
