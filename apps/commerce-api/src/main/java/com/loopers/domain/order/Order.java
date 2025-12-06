package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_user_status", columnList = "user_id, status"),
                @Index(name = "idx_user_created_at", columnList = "user_id, created_at DESC")
        }
)
public class Order extends BaseEntity {

    private String userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private long totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    protected Order() {
    }

    private Order(String userId, List<OrderItem> items, long totalAmount) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;

        for (OrderItem item : items) {
            addOrderItem(item);
        }
    }

    public static Order create(String userId, List<OrderItem> items, long totalAmount) {
        return new Order(userId, items, totalAmount);
    }

    private void addOrderItem(OrderItem item) {
        this.orderItems.add(item);
        item.setOrder(this);
    }

    public void startPayment() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "PENDING 상태에서만 결제를 시작할 수 있습니다."
            );
        }
        this.status = OrderStatus.PAYING;
    }

    public void confirm() {
        if (this.status == OrderStatus.CONFIRMED) {
            log.warn("Order is already CONFIRMED. Idempotent operation. orderId={}", this.getId());
            return;
        }

        if (this.status != OrderStatus.PAYING) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "PAYING 상태에서만 확정할 수 있습니다."
            );
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void fail() {
        if (this.status == OrderStatus.FAILED) {
            log.warn("Order is already FAILED. Idempotent operation. orderId={}", this.getId());
            return;
        }

        if (this.status != OrderStatus.PAYING) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "PAYING 상태에서만 실패 처리할 수 있습니다."
            );
        }
        this.status = OrderStatus.FAILED;
    }

    public String getUserId() {
        return userId;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
