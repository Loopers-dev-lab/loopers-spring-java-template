package com.loopers.core.service.order.command;

import lombok.Getter;

import java.util.List;

@Getter
public class OrderProductsCommand {

    private final String userIdentifier;

    private final List<OrderItem> orderItems;

    public OrderProductsCommand(String userIdentifier, List<OrderItem> orderItems) {
        this.userIdentifier = userIdentifier;
        this.orderItems = orderItems;
    }

    @Getter
    public static class OrderItem {

        private final String productId;

        private final Long quantity;

        public OrderItem(String productId, Long quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}
