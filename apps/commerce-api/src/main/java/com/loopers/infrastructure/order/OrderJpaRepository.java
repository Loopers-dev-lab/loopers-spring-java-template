package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderModel, Long> {
    // TODO
}
