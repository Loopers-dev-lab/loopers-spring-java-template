package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "orders")
@Entity
public class Order extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public static Order create(final Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유저는 비어있을 수 없습니다.");
        }
        Order order = new Order();
        order.userId = userId;
        order.orderStatus = OrderStatus.PENDING;
        return order;
    }

    public void paid() {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제승인은 대기상태에서만 가능합니다.");
        }
        this.orderStatus = OrderStatus.PAID;
    }

    public void fail() {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제실패는 대기상태에서만 가능합니다.");
        }
        this.orderStatus = OrderStatus.FAILED;
    }
}
