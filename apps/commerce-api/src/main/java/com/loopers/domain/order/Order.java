package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
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

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "PENDING 상태에서만 확정할 수 있습니다."
            );
        }
        this.status = OrderStatus.CONFIRMED;
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
