package com.loopers.core.service.order.command;

import java.util.List;

public record OrderProductsCommand(String userIdentifier, List<OrderItem> orderItems) {

    public record OrderItem(String productId, Long quantity) {

    }
}
