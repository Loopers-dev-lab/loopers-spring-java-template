package com.loopers.application.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderInfo {
    
    public record OrderItem(
            Long orderId,
            String orderNumber,
            Long userId,
            String status,
            BigDecimal totalPrice,
            LocalDateTime createdAt
    ) {
    }

    public record OrderDetail(
            Long orderId,
            String orderNumber,
            Long userId,
            String status,
            BigDecimal totalPrice,
            LocalDateTime createdAt,
            List<OrderItemDetail> orderItems
    ) {
        public record OrderItemDetail(
                Long productId,
                String productName,
                Long optionId,
                String optionName,
                BigDecimal quantity,
                BigDecimal price,
                String productImageUrl
        ) {
        }
    }
    
    /**
     * 주문 목록 응답
     */
    public record ListResponse(
            List<OrderItem> orders,
            int totalPages,
            int currentPage,
            int size
    ) {
    }
}
