package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    @Override
    public List<Order> findOrdersByUserId(Long userId) {
        return List.of();
    }

    @Override
    public Optional<Order> findByIdAndUserId(Long orderId, Long userId) {
        return Optional.empty();
    }

    @Override
    public void saveOrder(Order order) {

    }

}
