package com.loopers.core.infra.database.mysql.order.entity;

import com.loopers.core.domain.order.OrderedProduct;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.ProductId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderedProductEntity {

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Long quantity;

    public static OrderedProductEntity from(OrderedProduct orderedProduct) {
        return new OrderedProductEntity(
            orderedProduct.getProductId().value(),
            orderedProduct.getQuantity().value()
        );
    }

    public OrderedProduct to() {
        return new OrderedProduct(
            new ProductId(this.productId),
            new Quantity(this.quantity)
        );
    }
}
