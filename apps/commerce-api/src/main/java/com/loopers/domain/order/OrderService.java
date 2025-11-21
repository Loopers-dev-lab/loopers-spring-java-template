package com.loopers.domain.order;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * 주문 도메인 서비스
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 주문을 생성합니다.
     *
     * @param request 주문 생성 요청
     * @return 생성된 주문 엔티티
     */
    @Transactional
    public OrderEntity createOrder(OrderDomainCreateRequest request) {
        OrderEntity order = OrderEntity.createOrder(request);
        return orderRepository.save(order);
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 조회된 주문 엔티티
     * @throws CoreException 주문을 찾을 수 없는 경우
     */
    public OrderEntity getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        String.format("주문을 찾을 수 없습니다. (ID: %d)", orderId)
                ));
    }

    /**
     * 주문 항목을 생성합니다.
     *
     * @param request 주문 항목 생성 요청
     * @return 생성된 주문 항목 엔티티
     */
    @Transactional
    public OrderItemEntity createOrderItem(OrderItemDomainCreateRequest request) {
        OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);
        return orderItemRepository.save(orderItem);
    }

    /**
     * 주문 ID로 주문 항목 목록을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 목록
     */
    public List<OrderItemEntity> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    /**
     * Soft-deletes the order identified by the given ID and persists related order items' updated state.
     *
     * @param orderId the ID of the order to delete
     * @throws CoreException if an order with the given ID does not exist
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        OrderEntity order = getOrderById(orderId);
        order.delete();

        List<OrderItemEntity> orderItemsByOrderId = getOrderItemsByOrderId(orderId);

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItemsByOrderId);
    }

    /**
     * 사용자 ID로 주문 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    public Page<OrderEntity> getOrdersByUserId(
            Long userId,
            Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    /**
     * 사용자 ID와 주문 상태로 주문 목록을 페이징하여 조회합니다.
     *
     * @param userId   사용자 ID
     * @param status   주문 상태
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    public Page<OrderEntity> getOrdersByUserIdAndStatus(
            Long userId,
            OrderStatus status,
            Pageable pageable) {
        return orderRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    /**
     * 주문 ID로 주문 항목 개수를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 개수
     */
    public int countOrderItems(Long orderId) {
        return orderItemRepository.countByOrderId(orderId);
    }

    /**
     * 주문 ID로 주문 항목 목록을 페이징하여 조회합니다.
     *
     * @param orderId  주문 ID
     * @param pageable 페이징 정보
     * @return 페이징된 주문 항목 목록
     */
    public Page<OrderItemEntity> getOrderItemsByOrderId(
            Long orderId,
            Pageable pageable) {
        return orderItemRepository.findByOrderId(orderId, pageable);
    }
}