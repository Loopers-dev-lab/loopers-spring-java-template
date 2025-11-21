package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessages;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Builder
    public Order(String userId, List<OrderItem> orderItems, OrderStatus status) {
        validateUserId(userId);
        validateOrderItems(orderItems);
        this.userId = userId;
        this.orderItems = orderItems;
        this.status = OrderStatus.CREATED;
    }

    //주문 리스트 유효성 검증
    private void validateOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_ORDER_ITEMS_LIST);
        }
    }

    //주문 유저 아이디 유효성 검증
    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_NAME_FORMAT);
        }
    }

    public static Order createOrder(String userId, List<OrderItem> orderItems) {
        return new Order(userId, orderItems, OrderStatus.CREATED);
    }
}
