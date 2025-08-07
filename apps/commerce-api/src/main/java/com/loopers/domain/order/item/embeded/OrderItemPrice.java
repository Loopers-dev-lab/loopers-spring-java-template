package com.loopers.domain.order.item.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class OrderItemPrice {
    @Column(name = "price_per_unit", nullable = false)
    private BigDecimal unitPrice;

    public OrderItemPrice() {

    }

    private OrderItemPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "단가는 0 이상이어야 합니다.");
        }
        this.unitPrice = price;
    }

    public static OrderItemPrice of(BigDecimal price) {
        return new OrderItemPrice(price);
    }

    public BigDecimal getValue() {
        return this.unitPrice;
    }
}
