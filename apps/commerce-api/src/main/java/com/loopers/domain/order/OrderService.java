package com.loopers.domain.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * packageName : com.loopers.domain.order
 * fileName     : OrderService
 * author      : byeonsungmun
 * date        : 2025. 11. 13.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 13.     byeonsungmun       최초 생성
 */
@Component
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }
}
