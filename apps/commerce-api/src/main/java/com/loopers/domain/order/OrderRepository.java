package com.loopers.domain.order;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(Long id);

    Page<Order> findByUserId(String userId, Pageable pageable);
}
