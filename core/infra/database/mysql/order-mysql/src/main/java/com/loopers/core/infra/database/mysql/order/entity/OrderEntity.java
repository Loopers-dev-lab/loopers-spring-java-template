package com.loopers.core.infra.database.mysql.order.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.user.vo.UserId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_user_id", columnList = "user_id"),
                @Index(name = "idx_order_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static OrderEntity from(Order order) {
        return new OrderEntity(
                Optional.ofNullable(order.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(order.getUserId().value())),
                order.getCreatedAt().value(),
                order.getUpdatedAt().value(),
                order.getDeletedAt().value()
        );
    }

    public Order to() {
        return Order.mappedBy(
                new OrderId(this.id.toString()),
                new UserId(this.userId.toString()),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt),
                new DeletedAt(this.deletedAt)
        );
    }
}
