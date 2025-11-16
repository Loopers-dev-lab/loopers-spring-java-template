package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final PointService pointService;

    @Transactional
    public OrderInfo createOrder(String userId, List<OrderService.OrderItemRequest> itemRequests) {
        validateItem(itemRequests);

        // 1. 상품 도메인 - 상품 조회
        List<Long> productIds = itemRequests.stream()
                .map(OrderService.OrderItemRequest::productId)
                .toList();

        List<Product> products = productIds.stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new CoreException(
                                ErrorType.NOT_FOUND,
                                "해당 상품을 찾을 수 없습니다."
                        )))
                .toList();

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 2. 주문 금액 계산 및 OrderItem 생성
        long totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderService.OrderItemRequest itemRequest : itemRequests) {
            Product product = productMap.get(itemRequest.productId());
            totalAmount += (long) product.getPrice() * itemRequest.quantity();

            orderItems.add(OrderItem.create(
                    product.getId(),
                    product.getName(),
                    itemRequest.quantity(),
                    product.getPrice()
            ));
        }

        // 3. 상품 도메인 - 재고 차감
        for (OrderService.OrderItemRequest itemRequest : itemRequests) {
            Product product = productMap.get(itemRequest.productId());

            if (!product.hasEnoughStock(itemRequest.quantity())) {
                throw new CoreException(
                        ErrorType.BAD_REQUEST,
                        String.format("상품 '%s'의 재고가 부족합니다.", product.getName())
                );
            }

            product.decreaseStock(itemRequest.quantity());
            productRepository.save(product);
        }

        // 4. 포인트 도메인 - 포인트 차감
        pointService.deduct(userId, totalAmount);

        // 5. 주문 도메인 - 주문 생성
        Order order = orderService.createOrder(userId, orderItems, totalAmount);

        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(String userId) {
        List<Order> orders = orderService.getOrders(userId);
        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrder(String userId, Long orderId) {
        Order order = orderService.getOrder(userId, orderId);
        return OrderInfo.from(order);
    }

    private static void validateItem(List<OrderService.OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "하나 이상의 상품을 주문해야 합니다."
            );
        }
    }
}
