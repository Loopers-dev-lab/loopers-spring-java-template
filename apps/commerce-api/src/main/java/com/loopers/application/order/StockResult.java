package com.loopers.application.order;

import java.util.List;

public record StockResult(
            List<OrderCommand.OrderLine> successLines,
            List<OrderCommand.OrderLine> failedLines,
            int requiringPrice,
            int errorPrice
    ) {
        public static StockResult of(
                List<OrderCommand.OrderLine> successLines,
                List<OrderCommand.OrderLine> failedLines,
                int requiringPrice,
                int errorPrice
        )  {
            return new StockResult(successLines,
                    failedLines,
                    requiringPrice,
                    errorPrice);
        }
}
