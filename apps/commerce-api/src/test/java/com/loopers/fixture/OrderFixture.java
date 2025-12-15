package com.loopers.fixture;

import com.loopers.domain.order.Order;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;

import java.util.List;

public class OrderFixture {

    // 빈 주문 생성
    public static Order empty(User user) {
        return Order.create(user);
    }

    // 단일 상품 주문
    public static Order withSingleItem(User user, Product product, int quantity) {
        Order order = Order.create(user);
        order.addOrderItem(product, quantity);
        return order;
    }

    // 다중 상품 주문
    public static Order withItems(User user, List<OrderItemData> items) {
        Order order = Order.create(user);
        for (OrderItemData item : items) {
            order.addOrderItem(item.product(), item.quantity());
        }
        return order;
    }

    // 쿠폰 적용 주문
    public static Order withCoupon(User user, Product product, int quantity, Long couponId) {
        Order order = Order.create(user);
        order.addOrderItem(product, quantity);
        order.applyCoupon(couponId);
        return order;
    }

    // 주문 아이템 데이터
    public record OrderItemData(Product product, int quantity) {
        public static OrderItemData of(Product product, int quantity) {
            return new OrderItemData(product, quantity);
        }
    }
}
