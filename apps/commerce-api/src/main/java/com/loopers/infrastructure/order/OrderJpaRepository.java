package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);
    List<Order> findAllByUserId(Long id);
}
