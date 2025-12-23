package com.loopers.application.order;

import com.loopers.domain.order.Order;
import java.util.List;

public class OrderEvent {

    public record CouponUsed(
            Long couponId,
            Long userId
    ) {
        public static CouponUsed from(Long couponId, Long userId) {
            return new CouponUsed(
                    couponId,
                    userId
            );
        }
    }

    public record OrderCreated(
            String loginId,
            String orderKey,
            Long orderId,
            Long userId,
            Integer originalTotalPrice,
            Integer discountPrice
    ) {
        public static OrderCreated from(Order order, String loginId) {
            return new OrderCreated(
                    loginId,
                    order.getOrderKey(),
                    order.getId(),
                    order.getUserId(),
                    order.getOriginalTotalPrice(),
                    order.getDiscountPrice()
            );
        }
    }

    public record OrderPaid(
            String orderKey,
            Long orderId,
            Long userId,
            Integer totalPrice,
            List<OrderItemInfo> orderItems
    ) {
        public static OrderPaid from(Order order) {
            List<OrderItemInfo> orderItemInfos = order.getOrderItems().stream()
                    .map(item -> new OrderItemInfo(
                            item.getProductId(),
                            item.getProductName(),
                            item.getPrice(),
                            item.getQuantity()
                    ))
                    .toList();
            
            return new OrderPaid(
                    order.getOrderKey(),
                    order.getId(),
                    order.getUserId(),
                    order.getOriginalTotalPrice() - (order.getDiscountPrice() != null ? order.getDiscountPrice() : 0),
                    orderItemInfos
            );
        }
    }

    public record OrderItemInfo(
            Long productId,
            String productName,
            Integer price,
            Integer quantity
    ) {
    }


}
