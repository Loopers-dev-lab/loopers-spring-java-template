package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.ProductModel;
import jakarta.persistence.*;

@Entity
@Table(name = "orders_item",
        uniqueConstraints =
        @UniqueConstraint(name = "uk_order_item",
                columnNames = {"orders_id", "product_id"}
        )
)
public class OrderItemModel extends BaseEntity {

    @Column(length = 50)
    private String name;
    private Integer price;
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private OrderItemStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orders_id", nullable = false)
    private OrderModel orders;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductModel product;

    protected OrderItemModel() {}

    public static OrderItemModel of(ProductModel product, int quantity) {
        OrderItemModel orderItem = new OrderItemModel();

        orderItem.product = product;
        orderItem.quantity = quantity;
        orderItem.name = product.getName();
        orderItem.price = product.getPrice();
        return orderItem;
    }

    public void setOrders(OrderModel orders) {
        this.orders = orders;
    }

    public void setStatus(OrderItemStatus status) {
        this.status = status;
    }

    public Integer getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public ProductModel getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
