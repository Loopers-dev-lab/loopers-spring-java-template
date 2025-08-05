package com.loopers.domain.order.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderItemOptionId {
    @Column(name = "option_id", nullable = false)
    private Long id;

    public OrderItemOptionId() {

    }

    private OrderItemOptionId(Long orderItemOptionId) {
        this.id = orderItemOptionId;
    }

    public static OrderItemOptionId of(Long orderItemOptionId) {
        if(orderItemOptionId == null){
            throw new CoreException(ErrorType.BAD_REQUEST, "orderItemOptionId cannot be null");
        }
        return new OrderItemOptionId(orderItemOptionId);
    }

    public Long getValue() {
        return id;
    }
}
