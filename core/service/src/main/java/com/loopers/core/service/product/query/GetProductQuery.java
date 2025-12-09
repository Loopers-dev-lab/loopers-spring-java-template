package com.loopers.core.service.product.query;

import lombok.Getter;

@Getter
public class GetProductQuery {

    private final String productId;

    public GetProductQuery(String productId) {
        this.productId = productId;
    }
}
