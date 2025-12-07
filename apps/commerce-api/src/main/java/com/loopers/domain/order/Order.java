package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;

    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "transaction_key")
    private String transactionKey;

    public Order(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유저 ID는 음수 혹은 null일 수 없습니다.");
        }
        this.userId = userId;
        this.items = new ArrayList<>();
        this.status = OrderStatus.CREATED;
    }

    public static Order create(Long userId) {
        return new Order(userId);
    }

    public void addItem(Long productId, int quantity, Money unitPrice, Money totalPrice) {
        this.items.add(OrderItem.create(productId, quantity, unitPrice, totalPrice));
    }

    public Money calculateTotalPrice() {
        return Money.sum(items.stream().map(OrderItem::getTotalPrice));
    }

    public void changeToFailed() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("주문 상태가 결제 중이 아닙니다.");
        }
        this.status = OrderStatus.FAILED;
    }

    public void changeToPaid() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("주문 상태가 결제 중이 아닙니다.");
        }
        this.status = OrderStatus.PAID;
    }

    public void changeToPending() {
        if (this.status != OrderStatus.CREATED && this.status != OrderStatus.FAILED) {
            throw new IllegalStateException("주문 상태가 등록/취소가 아닙니다.");
        }
        this.status = OrderStatus.PENDING;
    }

    public void setTransactionKey(String transactionKey) {
        this.transactionKey = transactionKey;
    }
}
