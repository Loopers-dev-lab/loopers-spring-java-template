package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessage;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderDomainService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(String userId, List<OrderItem> orderItems,  long totalAmount) {
        Order order = Order.create(userId, orderItems, totalAmount);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Order getOrder(String userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        ErrorMessage.ORDER_NOT_FOUND
                ));

        if (!order.getUserId().equals(userId)) {
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "해당 주문에 접근할 권한이 없습니다."
            );
        }

        return order;
    }

    @Transactional
    public void confirmOrder(String userId, Long orderId) {
        Order order = getOrder(userId, orderId);
        order.confirm();
    }

    @Transactional
    public void failOrder(String userId, Long orderId) {
        Order order = getOrder(userId, orderId);
        order.fail();
    }
}
