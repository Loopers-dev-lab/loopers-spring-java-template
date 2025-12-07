package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public Optional<Order> findOrder(Long id, Long userId) {
        return orderRepository.findByIdAndUserId(id, userId);
    }

    public Optional<Order> findOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> findAll(Long userId) {
        return orderRepository.findAllByUserId(userId);
    }

    public void saveTransactionKey(Long orderId, String transactionKey) {
        Order order = findOrderById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다. orderId: " + orderId));
        order.setTransactionKey(transactionKey);
    }

    public List<Order> findPendingOrders(Long duration) {
        ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(duration);
        return orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, threshold);
    }
}
