package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.user.vo.UserId;
import lombok.Getter;

import java.util.List;

@Getter
public class Order {

    private final OrderId orderId;

    private final UserId userId;

    private final List<OrderedProduct> orderedProducts;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    private Order(
            OrderId orderId,
            UserId userId,
            List<OrderedProduct> orderedProducts,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderedProducts = orderedProducts;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Order mappedBy(
            OrderId orderId,
            UserId userId,
            List<OrderedProduct> orderedProducts,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new Order(orderId, userId, orderedProducts, createdAt, updatedAt, deletedAt);
    }


    public static Order create(
            UserId userId,
            List<OrderedProduct> orderedProducts
    ) {
        return new Order(
                OrderId.empty(),
                userId,
                orderedProducts,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }
}
