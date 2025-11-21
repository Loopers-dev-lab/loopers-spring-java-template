package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.common.Money;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Embedded;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;

@Entity
@Table(name = "orderitems")
public class OrderItemModel extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private OrderModel order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductModel product;

    @Embedded
    private Quantity quantity;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "order_price"))
    private Money orderPrice;

    protected OrderItemModel() {
    }

    public OrderItemModel(ProductModel product, Quantity quantity, Money orderPrice) {
        this.product = product;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }

    public OrderModel getOrder() {
        return order;
    }

    public ProductModel getProduct() {
        return product;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Money getOrderPrice() {
        return orderPrice;
    }

    protected void setOrder(OrderModel order) {
        this.order = order;
    }
}