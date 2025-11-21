package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * packageName : com.loopers.infrastructure.order
 * fileName     : OrderJpaRepository
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
public interface OrderJpaRepository extends JpaRepository<Order, Long> {
}
