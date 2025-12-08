package com.loopers.domain.stock.event;

public record StockProcessEvent(
    Long orderId,
    boolean isSuccess,
    String reason
) {
    public static StockProcessEvent success(Long orderId) {
        return new StockProcessEvent(orderId, true, null);
    }

    public static StockProcessEvent failure(Long orderId, String reason) {
        return new StockProcessEvent(orderId, false, reason);
    }
}
