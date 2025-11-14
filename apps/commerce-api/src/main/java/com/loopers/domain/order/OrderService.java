package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 도메인 서비스
 * 주문 생성 시 Product, User 등 다른 도메인과의 협력을 처리한다
 */
public class OrderService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PointService pointService;

    public OrderService(ProductRepository productRepository,
                        UserRepository userRepository,
                        OrderRepository orderRepository,
                        PointService pointService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.pointService = pointService;
    }

    /**
     * 주문 생성
     * - 상품 존재 확인
     * - 재고 확인 및 차감
     * - 포인트 확인 및 차감
     * - 주문 저장
     *
     * @param userId     사용자 ID
     * @param orderItems 주문 항목 목록
     * @param usedPoints 사용할 포인트
     * @return 생성된 주문
     */
    public Order createOrder(String userId, List<OrderItemRequest> orderItems, int usedPoints) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));

        // 2. 상품 조회 및 재고 확인
        List<OrderItem> validatedOrderItems = orderItems.stream()
                .map(this::validateAndCreateOrderItem)
                .collect(Collectors.toList());

        // 3. 주문 생성 (총 금액 계산 포함)
        Order order = Order.create(userId, validatedOrderItems, usedPoints);

        // 4. 포인트 차감 (Point 도메인)
        if (usedPoints > 0) {
            pointService.consume(userId, (long) usedPoints);
        }

        // 5. 재고 차감
        for (OrderItemRequest request : orderItems) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + request.getProductId()));

            product.decreaseStock(request.getQuantity());
            productRepository.save(product);
        }

        // 6. 주문 저장
        return orderRepository.save(order);
    }

    /**
     * 주문 항목 검증 및 생성
     */
    private OrderItem validateAndCreateOrderItem(OrderItemRequest request) {
        // 상품 조회
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + request.getProductId()));

        // 재고 확인
        if (!product.canPurchase(request.getQuantity())) {
            throw new IllegalStateException(
                    String.format("재고가 부족합니다. 상품: %s, 요청: %d, 재고: %d",
                            product.getName(),
                            request.getQuantity(),
                            product.getStock())
            );
        }

        // OrderItem 생성
        return OrderItem.create(
                product.getId(),
                product.getName(),
                product.getPrice(),
                request.getQuantity()
        );
    }

    /**
     * 주문 취소
     * - 주문 상태 변경
     * - 재고 복구
     * - 포인트 복구
     *
     * @param orderId 주문 ID
     * @param userId  사용자 ID (권한 확인용)
     */
    public void cancelOrder(Long orderId, String userId) {
        // 1. 주문 조회
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));

        // 2. 권한 확인
        if (!order.isOwnedBy(userId)) {
            throw new IllegalStateException("본인의 주문만 취소할 수 있습니다");
        }

        // 3. 주문 취소 (도메인 로직)
        order.cancel();

        // 4. 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + item.getProductId()));

            product.increaseStock(item.getQuantity());
            productRepository.save(product);
        }

        // 5. 포인트 복구 (Point 도메인)
        if (order.getUsedPoints() > 0) {
            pointService.refund(userId, (long) order.getUsedPoints());
        }

        // 6. 주문 저장
        orderRepository.save(order);
    }

    /**
     * 주문 완료
     *
     * @param orderId 주문 ID
     */
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));

        order.complete();
        orderRepository.save(order);
    }

    /**
     * 주문 항목 요청 DTO
     */
    public static class OrderItemRequest {
        private final Long productId;
        private final int quantity;

        public OrderItemRequest(Long productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
