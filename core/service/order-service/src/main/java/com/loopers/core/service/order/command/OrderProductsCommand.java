package com.loopers.core.service.order.command;

import lombok.Getter;

import java.util.List;

@Getter
public class OrderProductsCommand {

    private final String userIdentifier;

    private final List<OrderProduct> products;

    public OrderProductsCommand(String userIdentifier, List<OrderProduct> products) {
        this.userIdentifier = userIdentifier;
        this.products = products;
    }

    @Getter
    public static class OrderProduct {

        private final String productId;

        private final Long quantity;

        public OrderProduct(String productId, Long quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}
