package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "orders")
@Getter
@Builder
public class Order extends BaseEntity {

    private Long userId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @Builder.Default
    List<OrderItem> orderItems = new ArrayList<>();

    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "receiverName", column = @Column(name = "receiver_name", nullable = false)),
            @AttributeOverride(name = "receiverPhoneNumber", column = @Column(name = "phone_number", nullable = false)),
            @AttributeOverride(name = "baseAddress", column = @Column(name = "base_address", nullable = false)),
            @AttributeOverride(name = "detailAddress", column = @Column(name = "detail_address", nullable = false))
    })
    private Delivery delivery;

    public Order() {
    }

    private Order(Long userId, List<OrderItem> orderItems, Integer totalPrice,
                 OrderStatus orderStatus, Delivery delivery) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalPrice = totalPrice;
        this.orderStatus = orderStatus;
        this.delivery = delivery;
    }

    public static Order createOrder(Long userId, Delivery delivery) {
        validateCreateOrder(userId, delivery);
        return Order.builder()
                .userId(userId)
                .totalPrice(0)
                .orderStatus(OrderStatus.CREATED)
                .delivery(delivery)
                .build();
    }

    public void addOrderItem(OrderItem orderItem) {
        validateOrderItem(orderItem);
        orderItems.add(orderItem);
    }

    public int addPrice(int price) {
        validatePrice(price);
        this.totalPrice += price;
        return this.totalPrice;
    }

    private static void validateCreateOrder(Long userId, Delivery delivery) {
        validateUserId(userId);
        validateDelivery(delivery);
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    private static void validateDelivery(Delivery delivery) {
        if (delivery == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "배송 정보는 필수입니다.");
        }
    }

    private void validateOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상품은 필수입니다.");
        }
    }

    private void validatePrice(int price) {
        if (price <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다.");
        }
    }
}
