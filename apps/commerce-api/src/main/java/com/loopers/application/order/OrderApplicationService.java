package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 Application Service
 * - 도메인 객체를 조합하여 유스케이스 흐름을 구성
 * - 트랜잭션 경계 관리
 * - DTO 변환
 */
public class OrderApplicationService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderApplicationService(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    /**
     * 주문 생성
     *
     * @param command 주문 생성 커맨드
     * @return 생성된 주문 응답
     */
    public OrderResponse createOrder(CreateOrderCommand command) {
        // 1. 도메인 서비스를 통한 주문 생성
        Order order = orderService.createOrder(
                command.getUserId(),
                command.getOrderItems(),
                command.getUsedPoints()
        );

        // 2. DTO 변환 및 반환
        return OrderResponse.from(order);
    }

    /**
     * 주문 상세 조회
     *
     * @param orderId 주문 ID
     * @param userId  사용자 ID (권한 확인용)
     * @return 주문 상세 응답
     */
    public OrderResponse getOrderDetail(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));

        // 권한 확인
        if (!order.isOwnedBy(userId)) {
            throw new IllegalStateException("본인의 주문만 조회할 수 있습니다");
        }

        return OrderResponse.from(order);
    }

    /**
     * 사용자의 주문 목록 조회
     *
     * @param userId 사용자 ID
     * @return 주문 목록
     */
    public List<OrderResponse> getUserOrders(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);

        return orders.stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 특정 상태 주문 목록 조회
     *
     * @param userId 사용자 ID
     * @param status 주문 상태
     * @return 주문 목록
     */
    public List<OrderResponse> getUserOrdersByStatus(String userId, OrderStatus status) {
        List<Order> orders = orderRepository.findByUserIdAndStatus(userId, status);

        return orders.stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 주문 취소
     *
     * @param orderId 주문 ID
     * @param userId  사용자 ID
     */
    public void cancelOrder(Long orderId, String userId) {
        orderService.cancelOrder(orderId, userId);
    }

    /**
     * 주문 완료 (관리자용)
     *
     * @param orderId 주문 ID
     */
    public void completeOrder(Long orderId) {
        orderService.completeOrder(orderId);
    }
}
