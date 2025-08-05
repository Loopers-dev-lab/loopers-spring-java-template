package com.loopers.application.order;

import java.math.BigDecimal;
import java.util.List;

public class OrderCommand {
    public static class Request {
        public record Create(
                Long userId,
                List<OrderItem> orderItems
        ) {
            public record OrderItem(
                    Long productId,
                    Long optionId,
                    int quantity,
                    BigDecimal pricePerUnit,
                    String productName,
                    String optionName,
                    String imageUrl
            ) {
            }
        }
        public record GetList(
                Long userId,
                String status, // 주문 상태 필터 (선택사항)
                int page,
                int size
        ) {
        }
        public record GetDetail(
                Long orderId,
                Long userId // 권한 확인용
        ) {
        }
    }
    public record OrderItemData(
            Long productId,
            Long optionId,
            int quantity,
            BigDecimal totalPrice,
            String productName,
            String optionName,
            String imageUrl
    ){

        public static OrderItemData of(Long id, Long optionId, int quantity, BigDecimal totalPrice, String productName, String optionName, String imageUrl) {
            return new OrderItemData(id, optionId, quantity, totalPrice, productName, optionName, imageUrl);
        }
    }
}
