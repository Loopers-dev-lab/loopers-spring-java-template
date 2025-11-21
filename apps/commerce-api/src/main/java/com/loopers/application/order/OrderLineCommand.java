package com.loopers.application.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderLineCommand {
    
    private final Long productId;
    private final int quantity;
    
    public static OrderLineCommand of(Long productId, int quantity) {
        return OrderLineCommand.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }
}