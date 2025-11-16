package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

/**
 * 주문 저장소 인터페이스.
 * <p>
 * Order 엔티티의 영속성 계층 접근을 추상화합니다.
 * </p>
 */
public interface OrderRepository {

    /**
     * 주문을 저장합니다.
     *
     * @param order 저장할 주문
     * @return 저장된 주문
     */
    Order save(Order order);

    /**
     * 주문 ID로 주문을 조회합니다.
     *
     * @param orderId 조회할 주문 ID
     * @return 조회된 주문
     */
    Optional<Order> findById(Long orderId);

    /**
     * 사용자 ID로 주문 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 주문 목록
     */
    List<Order> findAllByUserId(Long userId);
}


