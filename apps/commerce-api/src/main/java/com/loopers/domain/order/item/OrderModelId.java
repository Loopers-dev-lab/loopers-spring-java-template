package com.loopers.domain.order.item;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderModelId {
    @Column(name = "order_id", nullable = false)
    private Long id;
    public OrderModelId() {

    }

    private OrderModelId(Long id) {
        this.id = id;
    }
    public static OrderModelId of(Long orderModelId) {
        if(orderModelId == null){
            throw new CoreException(ErrorType.BAD_REQUEST, "orderModelId cannot be null");
        }
        return new OrderModelId(orderModelId);
    }

    public Long value() {
        return id;
    }
}
