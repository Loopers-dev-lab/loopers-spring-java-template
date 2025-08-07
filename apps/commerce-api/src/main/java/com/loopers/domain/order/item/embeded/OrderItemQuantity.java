package com.loopers.domain.order.item.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class OrderItemQuantity {
    
    @Column(name = "quantity", nullable = false)
    private int quantity;
    
    protected OrderItemQuantity() {}
    
    private OrderItemQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public static OrderItemQuantity of(int quantity) {
        validateQuantity(quantity);
        return new OrderItemQuantity(quantity);
    }
    
    public int getValue() {
        return this.quantity;
    }
    
    private static void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.");
        }
        if (quantity > 999) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 999개를 초과할 수 없습니다.");
        }
    }
}
