package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_no", columnList = "orderNo")
})
public class Order extends BaseEntity {

    @Getter
    @Column(name = "order_no", nullable = false, unique = true, length = 100)
    private String orderNo;

    @Getter
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Getter
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_price"))
    private Money totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    private Order(String orderNo, Long memberId, List<OrderItem> items, Money totalPrice, OrderStatus status) {
        validateMemberId(memberId);
        validateItems(items);
        validateTotalPrice(totalPrice);

        this.orderNo = orderNo;
        this.memberId = memberId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.items = new ArrayList<>();
        items.forEach(this::addItem);
    }

    public static Order create(Long memberId, List<OrderItem> items) {
        validateMemberId(memberId);
        validateItems(items);
        Money totalPrice = calculateTotalPrice(items);
        String orderNo = generateOrderNo();
        return new Order(orderNo, memberId, items, totalPrice, OrderStatus.PENDING_PAYMENT);
    }

    public static Order create(Long memberId, List<OrderItem> items, Money finalPrice) {
        validateMemberId(memberId);
        validateItems(items);
        validateTotalPrice(finalPrice);
        String orderNo = generateOrderNo();
        return new Order(orderNo, memberId, items, finalPrice, OrderStatus.PENDING_PAYMENT);
    }

    private static String generateOrderNo() {
        return "ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void addItem(OrderItem item) {
        this.items.add(item);
        item.assignOrder(this);
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private static Money calculateTotalPrice(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(Money.zero(), Money::plus);
    }

    private static void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }

    private static void validateItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 필수입니다");
        }
    }

    private static void validateTotalPrice(Money totalPrice) {
        if (totalPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "총 금액은 필수입니다.");
        }
    }

    public void markAsPaid() {
        if (this.status == OrderStatus.PAID) {
            return;
        }
        if (this.status == OrderStatus.CANCELLED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "취소된 주문은 결제 완료로 변경할 수 없습니다.");
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (this.status == OrderStatus.PAID) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 완료된 주문은 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isPendingPayment() {
        return this.status == OrderStatus.PENDING_PAYMENT;
    }

    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }

    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }
}
