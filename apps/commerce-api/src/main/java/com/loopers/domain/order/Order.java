package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Order extends BaseEntity {

    private BigDecimal finalAmount;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(
            BigDecimal finalAmount,
            BigDecimal totalPrice,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            OrderStatus orderStatus,
            User user,
            List<OrderItem> orderItems
    ) {
        this.finalAmount = finalAmount;
        this.totalPrice = totalPrice;
        this.discountAmount = discountAmount;
        this.shippingFee = shippingFee;
        this.orderStatus = orderStatus;
        this.user = user;
        if (orderItems != null) {
            this.orderItems = orderItems;
            orderItems.forEach(item -> item.setOrder(this));
        }
        guard();
    }

    public void setUser(User user) {
        if (this.user != null) {
            throw new CoreException(ErrorType.CONFLICT, "Order : User가 이미 존재합니다.");
        }
        this.user = user;
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void confirm() {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : PENDING 상태의 주문만 확인할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.CONFIRMED;
    }

    /**
     * Payment에서 계산된 최종 금액을 설정
     */
    public void setFinalAmount(BigDecimal finalAmount) {
        if (finalAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : finalAmount가 비어있을 수 없습니다.");
        }
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : finalAmount는 음수가 될 수 없습니다.");
        }
        this.finalAmount = finalAmount;
    }

    @Override
    protected void guard() {
        if (finalAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : finalAmount가 비어있을 수 없습니다.");
        } else if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : finalAmount는 음수가 될 수 없습니다.");
        }

        if (totalPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : totalPrice가 비어있을 수 없습니다.");
        } else if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : totalPrice는 음수가 될 수 없습니다.");
        }

        if (discountAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : discountAmount가 비어있을 수 없습니다.");
        } else if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : discountAmount는 음수가 될 수 없습니다.");
        }

        if (shippingFee == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : shippingFee가 비어있을 수 없습니다.");
        } else if (shippingFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : shippingFee는 음수가 될 수 없습니다.");
        }

        if (orderStatus == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : orderStatus가 비어있을 수 없습니다.");
        }

        if (orderItems == null || orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : orderItems가 비어있을 수 없습니다.");
        }
    }
}

