package com.loopers.fixture;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;

import java.util.List;

public class OrderFixture {

    public static Order create(String userId, long totalAmount) {
        return create(userId, totalAmount, 0L);
    }

    public static Order create(String userId, long totalAmount, long discountAmount) {
        OrderItem item = OrderItem.create(1L, "테스트 상품", 1L, totalAmount);
        return Order.create(userId, List.of(item), totalAmount, null, discountAmount);
    }

    public static Order createWithCoupon(String userId, long totalAmount, Long couponId, long discountAmount) {
        OrderItem item = OrderItem.create(1L, "테스트 상품", 1L, totalAmount);
        return Order.create(userId, List.of(item), totalAmount, couponId, discountAmount);
    }
}
