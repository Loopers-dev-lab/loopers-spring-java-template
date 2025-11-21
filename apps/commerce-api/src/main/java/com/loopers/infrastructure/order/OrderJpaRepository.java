package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

  /**
   * OrderItem과 함께 조회 (@EntityGraph로 N+1 방지)
   */
  @EntityGraph(attributePaths = "items")
  @Query("SELECT o FROM Order o WHERE o.id = :id")
  Optional<Order> findWithItemsById(@Param("id") Long id);

  /**
   * 사용자별 주문 목록 조회 (DTO 프로젝션, SIZE()로 개수만 조회)
   * 정렬은 Pageable로 제어
   */
  @Query("SELECT new com.loopers.domain.order.OrderListDto(" +
      "o.id, o.userId, o.status, o.totalAmount.value, o.orderedAt, SIZE(o.items)) " +
      "FROM Order o WHERE o.userId = :userId")
  Page<OrderListDto> findOrderListByUserId(@Param("userId") Long userId, Pageable pageable);
}
