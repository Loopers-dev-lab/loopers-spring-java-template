package com.loopers.core.service.product.query;

import lombok.Getter;

@Getter
public class GetProductDetailQuery {

    private final String productId;

    public GetProductDetailQuery(String productId) {
        this.productId = productId;
    }
}
