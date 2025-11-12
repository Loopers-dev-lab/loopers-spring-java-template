package com.loopers.domain.order;

import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;

    @Transactional
    public Order createOrder(String userId, List<OrderItemRequest> orderItemRequests) {
        // 1. 상품 조회 및 재고 확인
        List<Long> productIds = orderItemRequests.stream()
            .map(OrderItemRequest::productId)
            .toList();

        List<Product> products = productRepository.findByIdIn(productIds);

        if (products.size() != productIds.size()) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품이 포함되어 있습니다.");
        }

        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(
                product -> product.getId(),
                product -> product
            ));

        // 2. 주문 생성
        Order order = Order.builder()
            .userId(userId)
            .status(OrderStatus.PENDING)
            .build();

        // 3. 주문 항목 생성 및 재고 차감
        for (OrderItemRequest request : orderItemRequests) {
            Product product = productMap.get(request.productId());

            if (!product.isAvailable()) {
                throw new CoreException(ErrorType.BAD_REQUEST,
                    String.format("상품 '%s'은(는) 현재 판매 불가능합니다.", product.getName()));
            }

            // 재고 차감 (Product가 재고 검증 및 차감 수행)
            product.deductStock(request.quantity());

            OrderItem orderItem = OrderItem.builder()
                .product(product)
                .quantity(request.quantity())
                .price(product.getPrice())
                .build();

            order.addOrderItem(orderItem);
        }

        // 4. 총 금액 계산
        order.calculateTotalAmount();

        // 5. 포인트 차감 (PointService가 포인트 검증 및 차감 수행)
        pointService.usePoint(userId, order.getTotalAmount());

        // 6. 주문 저장
        return orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

        // 권한 확인
        if (!order.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "권한이 없습니다.");
        }

        // 취소 가능 여부 확인 (Order가 검증 수행)
        order.cancel();

        // 재고 복구
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            product.restoreStock(orderItem.getQuantity());
        }

        // 포인트 환불
        pointService.refundPoint(userId, order.getTotalAmount());
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersByUser(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    public record OrderItemRequest(Long productId, Integer quantity) {

    }
}
