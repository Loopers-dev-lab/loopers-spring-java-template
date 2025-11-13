package com.loopers.domain.orderproduct;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.Order;
import com.loopers.domain.product.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@Entity
@Table(name = "order_product")
public class OrderProduct extends BaseEntity {
    private int quantity;
    private BigDecimal price;
    private Order order;
    private Product product;

    public OrderProduct(Order order, List<Product> products) {
        return;
    }
}
