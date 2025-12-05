package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(Long userId) {
        Order order = Order.create(userId);
        return orderRepository.save(order);
    }

    @Transactional
    public List<OrderItem> createOrderItems(List<OrderItem> orderItems) {
        return orderRepository.saveOrderItems(orderItems);
    }

    public Order getPendingOrder(final Long userId, final Long orderId) {
        return orderRepository.findByIdAndUserIdAndOrderStatus(orderId, userId, OrderStatus.PENDING)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[orderId = " + orderId + "] 주문 대기중인 주문을 찾을 수 없습니다."));
    }

    public List<OrderItem> getOrderItemsByOrderId(final Long orderId) {
        List<OrderItem> list = orderRepository.findOrderItemsByOrderId(orderId);
        if (list.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "[orderId = " + orderId + "] 주문 상품을 찾을 수 없습니다.");
        }
        return list;
    }
}
