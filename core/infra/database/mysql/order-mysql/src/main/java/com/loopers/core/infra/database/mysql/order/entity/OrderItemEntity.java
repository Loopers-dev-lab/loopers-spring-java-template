package com.loopers.core.infra.database.mysql.order.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderItemId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.ProductId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_item_order_id", columnList = "order_id"),
                @Index(name = "idx_order_item_product_id", columnList = "product_id"),
                @Index(name = "idx_order_item_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static OrderItemEntity from(OrderItem orderItem) {
        return new OrderItemEntity(
                Optional.ofNullable(orderItem.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(orderItem.getOrderId().value())),
                Long.parseLong(Objects.requireNonNull(orderItem.getProductId().value())),
                orderItem.getQuantity().value(),
                orderItem.getCreatedAt().value(),
                orderItem.getUpdatedAt().value(),
                orderItem.getDeletedAt().value()
        );
    }

    public OrderItem to() {
        return OrderItem.mappedBy(
                new OrderItemId(this.id.toString()),
                new OrderId(this.orderId.toString()),
                new ProductId(this.productId.toString()),
                new Quantity(this.quantity),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt),
                new DeletedAt(this.deletedAt)
        );
    }
}
