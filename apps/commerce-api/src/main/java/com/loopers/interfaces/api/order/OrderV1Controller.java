package com.loopers.interfaces.api.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderV1Controller {
    private final OrderDomainService orderDomainService;
    private final OrderRepository orderRepository;

    @PostMapping("")
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @RequestHeader("X-USER-ID") String userId,
        @RequestBody OrderV1Dto.CreateOrderRequest request
    ) {
        List<OrderItem> items = request.items().stream()
            .map(i -> new OrderItem(null, i.productId(), i.quantity(), i.price()))
            .collect(Collectors.toList());
        Order order = orderDomainService.createOrder(userId, items);
        Order saved = orderRepository.save(order);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(saved));
    }

    @GetMapping("")
    public ApiResponse<OrderV1Dto.OrderListResponse> getOrders(
        @RequestHeader("X-USER-ID") String userId
    ) {
        List<Order> orders = orderDomainService.getOrdersByUserId(userId);
        List<OrderV1Dto.OrderSummary> items = orders.stream()
            .map(OrderV1Dto.OrderSummary::from)
            .collect(Collectors.toList());
        return ApiResponse.success(new OrderV1Dto.OrderListResponse(items));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderV1Dto.OrderResponse> getOrder(
        @PathVariable("orderId") Long orderId
    ) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다."));
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(order));
    }
}
