package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

/**
 * 주문 Repository Interface
 * Domain Layer에 위치하며, 구현체는 Infrastructure Layer에 존재한다
 */
public interface OrderRepository {

    /**
     * 주문 저장
     *
     * @param order 저장할 주문
     * @return 저장된 주문 (ID 포함)
     */
    Order save(Order order);

    /**
     * 주문 ID로 조회
     *
     * @param orderId 주문 ID
     * @return 주문 (존재하지 않으면 empty)
     */
    Optional<Order> findById(Long orderId);

    /**
     * 사용자의 주문 목록 조회
     *
     * @param userId 사용자 ID
     * @return 주문 목록
     */
    List<Order> findByUserId(String userId);

    /**
     * 특정 상품이 포함된 주문 목록 조회
     *
     * @param productId 상품 ID
     * @return 주문 목록
     */
    List<Order> findByProductId(Long productId);

    /**
     * 주문 상태로 조회
     *
     * @param status 주문 상태
     * @return 주문 목록
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * 사용자의 특정 상태 주문 조회
     *
     * @param userId 사용자 ID
     * @param status 주문 상태
     * @return 주문 목록
     */
    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);

    /**
     * 주문 존재 여부 확인
     *
     * @param orderId 주문 ID
     * @return 존재 여부
     */
    boolean existsById(Long orderId);

    /**
     * 모든 주문 조회
     *
     * @return 모든 주문 목록
     */
    List<Order> findAll();
}
