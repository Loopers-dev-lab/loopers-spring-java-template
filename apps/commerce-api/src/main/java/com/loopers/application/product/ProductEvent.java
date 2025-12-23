package com.loopers.application.product;

import java.time.ZonedDateTime;

public class ProductEvent {

    public record StockDepleted(
            Long productId,
            Integer remainingStock,
            ZonedDateTime timestamp
    ) {
        public static StockDepleted from(Long productId, Integer remainingStock) {
            return new StockDepleted(
                    productId,
                    remainingStock,
                    ZonedDateTime.now()
            );
        }
    }
}

