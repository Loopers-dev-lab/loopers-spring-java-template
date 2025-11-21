package com.loopers.domain.orderitem;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public List<OrderItem> getOrderItemsByOrder(Long orderId) {
        return orderItemRepository.getOrderItemsByOrder(orderId);
    }
}
