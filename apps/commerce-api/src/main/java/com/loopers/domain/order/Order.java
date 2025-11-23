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

    public void changeToPaid() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("이미 결제된 주문입니다.");
        }
        this.status = OrderStatus.PAID;
    }
}
