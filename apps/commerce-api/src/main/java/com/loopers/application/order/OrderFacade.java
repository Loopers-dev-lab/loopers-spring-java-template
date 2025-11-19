package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductDomainService;
import com.loopers.interfaces.api.order.OrderDto;
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

    private final OrderDomainService orderDomainService;
    private final ProductDomainService productDomainService;
    private final PointAccountDomainService pointAccountDomainService;

    @Transactional
    public OrderInfo createOrder(String userId, List<OrderDto.OrderItemRequest> itemRequests) {
        validateItem(itemRequests);

        // 상품 조회
        List<Long> productIds = itemRequests.stream()
                .map(OrderDto.OrderItemRequest::productId)
                .toList();

        List<Product> products = productDomainService.findByIds(productIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 주문 금액 계산 및 OrderItem 생성
        long totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderDto.OrderItemRequest itemRequest : itemRequests) {
            Product product = productMap.get(itemRequest.productId());
            totalAmount += product.getPrice() * itemRequest.quantity();

            orderItems.add(OrderItem.create(
                    product.getId(),
                    product.getName(),
                    itemRequest.quantity(),
                    product.getPrice()
            ));
        }

        // 재고 차감
        for (OrderDto.OrderItemRequest itemRequest : itemRequests) {
            productDomainService.decreaseStock(
                    itemRequest.productId(),
                    itemRequest.quantity()
            );
        }

        // 포인트 차감
        pointAccountDomainService.deduct(userId, totalAmount);

        // 주문 생성
        Order order = orderDomainService.createOrder(userId, orderItems, totalAmount);

        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(String userId) {
        List<Order> orders = orderDomainService.getOrders(userId);
        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrder(String userId, Long orderId) {
        Order order = orderDomainService.getOrder(userId, orderId);
        return OrderInfo.from(order);
    }

    private static void validateItem(List<OrderDto.OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "하나 이상의 상품을 주문해야 합니다."
            );
        }
    }
}
