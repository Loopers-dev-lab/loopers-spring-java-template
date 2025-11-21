package com.loopers.domain.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;

    public Order createOrder(Long userId, Delivery delivery) {
        return Order.createOrder(userId, delivery);
    }

    public void createOrderItem(Order order, Product product, Integer quantity) {
        OrderItem orderItem = OrderItem.createOrderItem(
                product.getId(),
                product.getName(),
                product.getPrice().getPrice(),
                quantity
        );

        order.addOrderItem(orderItem);
    }

    public int addTotalPrice(Order order, int price, int quantity) {
        return order.addPrice(price * quantity);
    }

    public void applyCoupon(Order order, Coupon coupon, int discountPrice) {
        order.applyCoupon(coupon.getId(), discountPrice);
    }

    public void saveOrder(Order order) {
        orderRepository.saveOrder(order);
    }

    public List<Order> findOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findOrdersByUserId(userId);

        if (orders.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "해당 사용자의 주문 내역이 없습니다.");
        }

        return orders;
    }

    public Optional<Order> findOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findOrderByIdAndUserId(orderId, userId);
    }
}
