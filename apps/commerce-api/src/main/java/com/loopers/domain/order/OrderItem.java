package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long productId;
    private String productName;
    private Long quantity;
    private int price;

    protected OrderItem() {
    }

    private OrderItem(Long productId, String productName, Long quantity, int price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public static OrderItem create(Long productId, String productName, Long quantity, int price) {
        return new OrderItem(productId, productName, quantity, price);
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Long getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }
}
