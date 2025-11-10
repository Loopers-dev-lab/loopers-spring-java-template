package com.loopers.core.domain.order;

import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.ProductId;
import lombok.Getter;

@Getter
public class OrderedProduct {

    private final ProductId productId;

    private final Quantity quantity;

    public OrderedProduct(ProductId productId, Quantity quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
