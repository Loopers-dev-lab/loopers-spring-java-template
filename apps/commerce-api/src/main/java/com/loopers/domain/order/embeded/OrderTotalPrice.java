package com.loopers.domain.order.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

@Embeddable
@Getter
public class OrderTotalPrice {
    
    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;
    
    protected OrderTotalPrice() {}
    
    private OrderTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public static OrderTotalPrice of(BigDecimal totalPrice) {
        validateTotalPrice(totalPrice);
        return new OrderTotalPrice(totalPrice);
    }
    
    public static OrderTotalPrice zero() {
        return new OrderTotalPrice(BigDecimal.ZERO);
    }
    
    public OrderTotalPrice add(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "추가할 금액은 0 이상이어야 합니다.");
        }
        return new OrderTotalPrice(this.totalPrice.add(amount));
    }
    
    public boolean isZero() {
        return this.totalPrice.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isPositive() {
        return this.totalPrice.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BigDecimal getValue() {
        return totalPrice;
    }
    
    private static void validateTotalPrice(BigDecimal totalPrice) {
        if (totalPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "총 금액은 필수입니다.");
        }
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "총 금액은 0 이상이어야 합니다.");
        }
    }
}