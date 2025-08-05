package com.loopers.domain.order.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

@Embeddable
@Getter
public class OrderItemQuantity {
    
    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;
    
    protected OrderItemQuantity() {}
    
    private OrderItemQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public static OrderItemQuantity of(BigDecimal quantity) {
        validateQuantity(quantity);
        return new OrderItemQuantity(quantity);
    }
    
    public BigDecimal getValue() {
        return this.quantity;
    }
    
    private static void validateQuantity(BigDecimal quantity) {
        if (quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 필수입니다.");
        }
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.");
        }
        if (quantity.compareTo(new BigDecimal("999")) > 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 999개를 초과할 수 없습니다.");
        }
    }
}
