package com.loopers.domain.order.item.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderItemProductId {
    @Column(name = "product_id", nullable = false)
    private Long id;
    public OrderItemProductId() {

    }

    private OrderItemProductId(Long id) {
        this.id = id;
    }
    public static OrderItemProductId of(Long id) {
        if(id == null){
            throw new CoreException(ErrorType.BAD_REQUEST, "id cannot be null");
        }
        return new OrderItemProductId(id);
    }
    public Long getValue() {
        return this.id;
    }

}
