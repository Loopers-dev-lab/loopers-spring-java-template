package com.loopers.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepository {


  Optional<Order> findById(Long id);

  /**
   * 주문 ID로 OrderItem과 함께 조회 (@EntityGraph 사용)
   */
  Optional<Order> findWithItemsById(Long id);

  /**
   * 사용자별 주문 목록 조회 (DTO 프로젝션, 페이징)
   */
  Page<OrderListDto> findOrderList(Long userId, Pageable pageable);

  Order save(Order order);
}
