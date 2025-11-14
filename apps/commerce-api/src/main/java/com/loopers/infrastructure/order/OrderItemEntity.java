package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 항목 JPA Entity
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // === 생성자 ===

    private OrderItemEntity(OrderEntity order, Long productId, String productName,
                            BigDecimal price, Integer quantity, BigDecimal subtotal,
                            LocalDateTime createdAt) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.createdAt = createdAt;
    }

    // === Domain <-> Entity 변환 ===

    /**
     * Domain 객체로부터 Entity 생성
     */
    public static OrderItemEntity from(OrderItem orderItem, OrderEntity order) {
        return new OrderItemEntity(
                order,
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getPrice(),
                orderItem.getQuantity(),
                orderItem.getSubtotal(),
                orderItem.getCreatedAt()
        );
    }

    /**
     * Entity를 Domain 객체로 변환
     */
    public OrderItem toDomain() {
        return OrderItem.reconstruct(
                this.id,
                this.productId,
                this.productName,
                this.price,
                this.quantity,
                this.createdAt
        );
    }
}
