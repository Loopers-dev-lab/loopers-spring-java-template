package com.loopers.core.service.order.query;

import lombok.Getter;

@Getter
public class GetOrderDetailQuery {

    private final String orderId;

    public GetOrderDetailQuery(String orderId) {
        this.orderId = orderId;
    }
}
