package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderItemId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.ProductId;
import lombok.Getter;

@Getter
public class OrderItem {

    private final OrderItemId id;

    private final OrderId orderId;

    private final ProductId productId;

    private final Quantity quantity;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    private OrderItem(
            OrderItemId id,
            OrderId orderId,
            ProductId productId,
            Quantity quantity,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static OrderItem create(
            OrderId orderId,
            ProductId productId,
            Quantity quantity
    ) {
        return new OrderItem(
                OrderItemId.empty(),
                orderId,
                productId,
                quantity,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }

    public static OrderItem mappedBy(
            OrderItemId id,
            OrderId orderId,
            ProductId productId,
            Quantity quantity,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new OrderItem(id, orderId, productId, quantity, createdAt, updatedAt, deletedAt);
    }
}
