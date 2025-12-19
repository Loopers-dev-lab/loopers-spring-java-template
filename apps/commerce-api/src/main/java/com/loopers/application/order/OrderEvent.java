package com.loopers.application.order;

import com.loopers.domain.order.Order;

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
            Integer originalTotalPrice,
            Integer discountPrice
    ) {
        public static OrderCreated from(Order order, String loginId) {
            return new OrderCreated(
                    loginId,
                    order.getOrderKey(),
                    order.getOriginalTotalPrice(),
                    order.getDiscountPrice()
            );
        }
    }


}
