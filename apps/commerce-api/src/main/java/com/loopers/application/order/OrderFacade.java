package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class OrderFacade {
    private final UserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final PointService pointService;
    private final SupplyService supplyService;

    public OrderInfo getOrderInfo(String userId, Long orderId) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Order order = orderService.getOrderByIdAndUserId(orderId, user.getId());

        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderInfo> getOrderList(String userId, Pageable pageable) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Page<Order> orders = orderService.getOrdersByUserId(user.getId(), pageable);
        return orders.map(OrderInfo::from);
    }

    @Transactional
    public OrderInfo createOrder(String userId, OrderV1Dto.OrderRequest request) {
        User user = userService.findByUserId(userId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // request에서 productId - quantity 맵 생성
        Map<Long, Integer> productQuantityMap = request.items().stream()
                .collect(Collectors.toMap(
                        OrderV1Dto.OrderRequest.OrderItemRequest::productId,
                        OrderV1Dto.OrderRequest.OrderItemRequest::quantity
                ));

        Map<Long, Product> productMap = productService.getProductMapByIds(productQuantityMap.keySet());

        request.items().forEach(item -> {
            supplyService.checkAndDecreaseStock(item.productId(), item.quantity());
        });

        Integer totalAmount = productService.calculateTotalAmount(productQuantityMap);

        pointService.checkAndDeductPoint(user.getId(), totalAmount);

        List<OrderItem> orderItems = request.items()
                .stream()
                .map(item -> OrderItem.create(
                        item.productId(),
                        productMap.get(item.productId()).getName(),
                        item.quantity(),
                        productMap.get(item.productId()).getPrice()
                ))
                .toList();
        Order order = Order.create(user.getId(), orderItems);

        orderService.save(order);

        return OrderInfo.from(order);
    }
}
