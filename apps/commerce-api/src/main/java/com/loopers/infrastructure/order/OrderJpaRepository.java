package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}

