package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 JPA Entity
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Integer usedPoints;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    // === 생성자 ===

    private OrderEntity(String userId, BigDecimal totalAmount, Integer usedPoints,
                        BigDecimal finalAmount, OrderStatus status,
                        LocalDateTime orderedAt, LocalDateTime modifiedAt) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.status = status;
        this.orderedAt = orderedAt;
        this.modifiedAt = modifiedAt;
    }

    // === Domain <-> Entity 변환 ===

    /**
     * Domain 객체로부터 Entity 생성
     */
    public static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity(
                order.getUserId(),
                order.getTotalAmount(),
                order.getUsedPoints(),
                order.getFinalAmount(),
                order.getStatus(),
                order.getOrderedAt(),
                order.getModifiedAt()
        );

        // OrderItem 변환 및 연관관계 설정
        List<OrderItemEntity> itemEntities = order.getOrderItems().stream()
                .map(item -> OrderItemEntity.from(item, entity))
                .collect(Collectors.toList());

        entity.orderItems.addAll(itemEntities);

        return entity;
    }

    /**
     * Entity를 Domain 객체로 변환
     */
    public Order toDomain() {
        List<OrderItem> domainItems = this.orderItems.stream()
                .map(OrderItemEntity::toDomain)
                .collect(Collectors.toList());

        return Order.reconstruct(
                this.id,
                this.userId,
                domainItems,
                this.totalAmount,
                this.usedPoints,
                this.finalAmount,
                this.status,
                this.orderedAt,
                this.modifiedAt
        );
    }

    /**
     * Domain 객체로부터 Entity 업데이트
     */
    public void updateFrom(Order order) {
        this.status = order.getStatus();
        this.modifiedAt = order.getModifiedAt();
    }
}
