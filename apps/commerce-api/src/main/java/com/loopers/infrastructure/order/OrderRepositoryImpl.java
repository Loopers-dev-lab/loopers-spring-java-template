package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderListDto;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

  private final OrderJpaRepository jpaRepository;

  @Override
  public Optional<Order> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public Optional<Order> findWithItemsById(Long id) {
    return jpaRepository.findWithItemsById(id);
  }

  @Override
  public Page<OrderListDto> findOrderList(Long userId, Pageable pageable) {
    return jpaRepository.findOrderListByUserId(userId, pageable);
  }

  @Override
  public Order save(Order order) {
    return jpaRepository.save(order);
  }
}
