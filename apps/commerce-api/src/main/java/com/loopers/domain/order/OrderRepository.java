package com.loopers.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepository {
  Optional<Order> findById(Long id);

  Page<Order> findByUserId(Long userId, Pageable pageable);

  Order save(Order order);

}
