package com.loopers.domain.order;

import java.util.Optional;

/**
 * packageName : com.loopers.domain.order
 * fileName     : OrderRepository
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long orderId);
}
