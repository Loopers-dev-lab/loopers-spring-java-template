package com.loopers.domain.order.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderUserId {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    private OrderUserId(Long userId) {
        this.userId = userId;
    }

    public OrderUserId() {

    }

    public static OrderUserId of(Long userId) {
        if(userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId cannot be null");
        }
        return new OrderUserId(userId);
    }

    public Long getValue() {
        return this.userId;
    }
}
