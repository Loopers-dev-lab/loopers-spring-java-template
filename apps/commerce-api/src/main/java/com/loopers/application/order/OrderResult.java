package com.loopers.application.order;

import java.util.List;

public class OrderResult {
    public record PreOrderResult(
            String userId,
            int requiringPoints,
            List<OrderCommand.OrderLine> successLines,
            List<OrderCommand.OrderLine> failedLines
    ) {
        public static PreOrderResult of(String userId,
                                        int successPoint,
                                        List<OrderCommand.OrderLine> successLines,
                                        List<OrderCommand.OrderLine> failedLines) {
            return new PreOrderResult(userId, successPoint, successLines, failedLines);
        }
    }

    public record PlaceOrderResult(
            String userId,
            Long orderId,
            int normalPrice,
            int errorPrice,
            List<OrderCommand.OrderLine> successLines,
            List<OrderCommand.OrderLine> failedLines
    ) {
        public static PlaceOrderResult of(String userId,
                                          Long orderId,
                                          int normalPrice,
                                          int errorPrice,
                                        List<OrderCommand.OrderLine> successLines,
                                        List<OrderCommand.OrderLine> failedLines) {
            return new PlaceOrderResult(
                    userId,
                    orderId,
                    normalPrice,
                    errorPrice,
                    successLines,
                    failedLines);
        }
    }

}
