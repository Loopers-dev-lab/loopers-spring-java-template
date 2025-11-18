package com.loopers.domain.order;

import com.loopers.application.order.OrderLineCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderModel putOrder(List<OrderLineCommand> orderLines) {
        // TODO
        return null;
    }


    public void putFailStatus(OrderModel order, List<OrderLineCommand> orderLineCommands) {
        // TODO 클린 아키텍처 고려하기
    }
}
