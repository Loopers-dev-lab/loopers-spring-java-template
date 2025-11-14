package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderItem extends BaseEntity {

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder
    private OrderItem(
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            Order order,
            Product product
    ) {
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.order = order;
        this.product = product;
        guard();
    }

    public void setOrder(Order order) {
        if (this.order != null && !this.order.equals(order)) {
            throw new CoreException(ErrorType.CONFLICT, "OrderItem : Order가 이미 존재합니다.");
        }
        this.order = order;
    }

    @Override
    protected void guard() {
        if (quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : quantity가 비어있을 수 없습니다.");
        } else if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : quantity는 0보다 커야 합니다.");
        }

        if (unitPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : unitPrice가 비어있을 수 없습니다.");
        } else if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : unitPrice는 음수가 될 수 없습니다.");
        }

        if (totalAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : totalAmount가 비어있을 수 없습니다.");
        } else if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : totalAmount는 음수가 될 수 없습니다.");
        }

        if (product == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : product가 비어있을 수 없습니다.");
        }
    }
}

