package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * packageName : com.loopers.domain.order
 * fileName     : Order
 * author      : byeonsungmun
 * date        : 2025. 11. 11.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 11.     byeonsungmun       최초 생성
 */
@Entity
@Table(name = "orders")
@Getter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    protected Order() {}

    private Order(String userId, OrderStatus status) {
        this.userId = requiredValidUserId(userId);
        this.totalAmount = 0L;
        this.status = requiredValidStatus(status);
        this.createdAt = LocalDateTime.now();
    }

    public static Order create(String userId) {
        return new Order(userId, OrderStatus.PENDING);
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
    }

    private OrderStatus requiredValidStatus(OrderStatus status) {
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상태는 필수 입니다.");
        }
        return status;
    }

    private String requiredValidUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수 입니다.");
        }
        return userId;
    }

    public void updateTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }
}
