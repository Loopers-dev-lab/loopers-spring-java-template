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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orders_id", nullable = false)
    private OrderModel orders;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductModel product;
}
