package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문 정보가 없습니다"));
    }

    public Order registerOrder(Order order) {
        return orderRepository.save(order);
    }

    public Order getOrderWithDetailsById(Long orderId) {
        return orderRepository.findOrderWithDetailsById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문 정보가 없습니다"));
    }
}
