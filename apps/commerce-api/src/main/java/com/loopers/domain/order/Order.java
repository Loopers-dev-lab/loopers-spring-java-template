package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.orderproduct.OrderProduct;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Entity
@Table(name = "orders")
@Getter
public class Order extends BaseEntity {

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false, columnDefinition = "int default 0")
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    @Builder
    public Order(List<Product> products, OrderStatus status, BigDecimal totalPrice, User user) {
        this.status = status;
        this.totalPrice = totalPrice;
        this.user = user;
        this.orderProducts = products.stream()
                .map(product -> new OrderProduct(this, products))
                .collect(Collectors.toList());
    }

    public static Order createOrder(List<Product> products) {
        return Order.builder()
                .products(products)
                .status(OrderStatus.INIT)
                .build();
    }
}
