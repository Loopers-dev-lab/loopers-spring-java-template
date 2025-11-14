package com.loopers.application.order;

import java.util.List;

public record StockDecreaseResult (
            List<OrderLineCommand> successLines,
            List<OrderLineCommand> failedLines,
            int totalAmount
    ) {
        public static StockDecreaseResult of(
                List<OrderLineCommand> successLines,
                List<OrderLineCommand> failedLines,
                int totalAmount
        )  {
            return new StockDecreaseResult(successLines, failedLines, totalAmount);
        }
}
