package com.loopers.infrastructure.orderitem;

import com.loopers.domain.orderitem.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Long> {
}
