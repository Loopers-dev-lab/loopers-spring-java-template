package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Embedded
    private OrderItemPrice orderItemPrice;

    private OrderItem(Product product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
        this.orderItemPrice = OrderItemPrice.of(product.getPriceValue());
    }

    public static OrderItem create(Product product, Integer quantity) {
        return new OrderItem(product, quantity);
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public OrderTotalAmount calculateAmount() {
        return OrderTotalAmount.of(this.orderItemPrice.getValue() * this.quantity);
    }

    public Long getUnitPriceValue() {
        return this.orderItemPrice.getValue();
    }
}
