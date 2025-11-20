package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {
}
