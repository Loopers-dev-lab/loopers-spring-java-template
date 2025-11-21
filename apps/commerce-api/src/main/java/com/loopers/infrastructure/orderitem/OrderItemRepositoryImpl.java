package com.loopers.infrastructure.orderitem;

import com.loopers.domain.orderitem.OrderItem;
import com.loopers.domain.orderitem.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderItemRepositoryImpl implements OrderItemRepository {

    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public List<OrderItem> getOrderItemsByOrder(Long orderId) {
        return orderItemJpaRepository.findByOrderId(orderId);
    }
}
