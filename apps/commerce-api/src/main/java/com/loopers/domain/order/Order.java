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

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "discount_amount")
    private Long discountAmount = 0L;

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

    public void applyCoupon(Long couponId) {
        this.couponId = couponId;
    }

    public void validatePayable() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "이미 결제가 완료된 주문입니다.");
        }

        if (this.status == OrderStatus.PAYMENT_PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "이미 결제 진행 중인 주문입니다.");
        }

        if (this.status == OrderStatus.CANCELLED) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "취소된 주문은 결제할 수 없습니다.");
        }

        if (this.status == OrderStatus.PAYMENT_FAILED) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "결제 실패한 주문입니다. 새로운 주문을 생성해주세요.");
        }

        if (this.orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "주문 항목이 비어있어 결제할 수 없습니다.");
        }
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

    public void markAsPaymentPending() {
        this.status = OrderStatus.PAYMENT_PENDING;
    }

    public void markAsPaymentFailed() {
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void markAsCancelled() {
        this.status = OrderStatus.CANCELLED;
    }

    public void markAsCompleted() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "이미 완료된 주문입니다.");
        }

        if (this.status == OrderStatus.CANCELLED) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "취소된 주문은 완료 처리할 수 없습니다.");
        }

        this.status = OrderStatus.COMPLETED;
        this.paidAt = ZonedDateTime.now();
    }
}
