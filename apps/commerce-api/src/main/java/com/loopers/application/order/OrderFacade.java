package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class OrderFacade {
    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final PointService pointService;

    @Transactional
    public OrderInfo createOrder(Long userId, List<OrderCreateItemInfo> orderCreateItemInfos) {
        User user = userService.getUser(userId);
        Order order = orderService.createOrder(userId);

        List<Long> productIdList = orderCreateItemInfos.stream()
                .map(OrderCreateItemInfo::productId)
                .toList();

        List<Product> products = productService.getProductIn(productIdList);

        Map<Long, Integer> itemQuantityMap = orderCreateItemInfos.stream()
                .collect(Collectors.toMap(OrderCreateItemInfo::productId, OrderCreateItemInfo::quantity));

        List<OrderItem> orderItemsData = products.stream()
                .map(product -> {
                    int quantity = itemQuantityMap.get(product.getId());
                    product.getStock().decrease(quantity);
                    return OrderItem.create(quantity, product.getPrice(), product.getId(), order.getId());
                }).toList();

        List<OrderItem> orderItems = orderService.createOrderItems(orderItemsData);

        Long totalPrice = orderItems.stream()
                .mapToLong(item -> item.getProductPrice() * item.getQuantity())
                .sum();

        List<OrderItemInfo> orderItemInfos = orderItems.stream()
                .map(OrderItemInfo::from)
                .toList();

        Point point = pointService.getPointByUserModelId(userId);
        point.usePoint(totalPrice);

        return OrderInfo.from(order, orderItemInfos);
    }
}
