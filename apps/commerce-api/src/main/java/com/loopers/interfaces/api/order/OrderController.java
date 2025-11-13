package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.OrderService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderApiSpec {

    private final OrderFacade orderFacade;

    @Override
    @PostMapping
    public ApiResponse<OrderDto.OrderResponse> createOrder(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody OrderDto.OrderCreateRequest request
    ) {
        List<OrderService.OrderItemRequest> items = request.items().stream()
                .map(item -> new OrderService.OrderItemRequest(item.productId(), item.quantity()))
                .toList();

        OrderInfo info = orderFacade.createOrder(userId, items);

        return ApiResponse.success(OrderDto.OrderResponse.from(info));
    }

    @Override
    @GetMapping
    public ApiResponse<OrderDto.OrderListResponse> getOrders(
            @RequestHeader("X-USER-ID") String userId
    ) {
        List<OrderInfo> orders = orderFacade.getOrders(userId);

        return ApiResponse.success(OrderDto.OrderListResponse.from(orders));
    }

    @Override
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDto.OrderResponse> getOrder(
            @RequestHeader("X-USER-ID") String userId, @PathVariable Long orderId) {
        OrderInfo info = orderFacade.getOrder(userId, orderId);

        return ApiResponse.success(OrderDto.OrderResponse.from(info));
    }
}
