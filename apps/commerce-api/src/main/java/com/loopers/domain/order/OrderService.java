package com.loopers.domain.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(Long userId, Delivery delivery) {
        String orderKey = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return Order.createOrder(userId, orderKey, delivery);
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

    public Order getOrderByOrderKey(String orderKey) {
        return orderRepository.findByOrderKey(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    public void validateIsUserOrder(String orderKey, Long userId) {
        Order order = orderRepository.findByOrderKey(orderKey)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        if (!order.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "본인의 주문만 결제할 수 있습니다.");
        }
    }

    public void payOrder(Order order) {
        order.pay();
    }

    public void cancelOrder(Order order) {
        order.cancel();
    }

    public void applyPrice(Order order, int originalTotalPrice, int discountPrice) {
        order.applyOriginalTotalPrice(originalTotalPrice);

        if (discountPrice > 0) {
            order.applyDiscountPrice(discountPrice);
        }
    }
}
