package com.loopers.application.order;

import java.util.List;

public class OrderCommand {

    public record Create(List<OrderItemRequest> orderItems) {

    }

    public record OrderItemRequest(Long productId, Integer quantity) {

    }
}
