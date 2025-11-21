package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Embedded
    private OrderTotalAmount totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "paid_at")
    private ZonedDateTime paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(User user) {
        validateUser(user);
        this.user = user;
        this.totalAmount = OrderTotalAmount.zero();
        this.status = OrderStatus.PENDING;
    }

    public static Order create(User user) {
        return new Order(user);
    }

    public void addOrderItem(Product product, Integer quantity) {
        if (product == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품은 필수입니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.");
        }

        OrderItem orderItem = OrderItem.create(product, quantity);
        this.orderItems.add(orderItem);
        orderItem.assignOrder(this);

        recalculateTotalAmount();
    }

    public void completePayment() {
        validateBeforePayment();
        this.status = OrderStatus.PAID;
        this.paidAt = ZonedDateTime.now();
    }

    private void recalculateTotalAmount() {
        this.totalAmount = this.orderItems.stream()
                .map(OrderItem::calculateAmount)
                .reduce(OrderTotalAmount.zero(), OrderTotalAmount::add);
    }

    private void validateBeforePayment() {
        if (this.orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.");
        }
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 필수입니다.");
        }
    }

    public Long getTotalAmountValue() {
        return this.totalAmount.getValue();
    }

    public List<OrderItem> getOrderItems() {
        return List.copyOf(orderItems);
    }
}
