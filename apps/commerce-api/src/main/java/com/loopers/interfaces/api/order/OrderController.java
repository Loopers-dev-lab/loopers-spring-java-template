package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping("/")
    @Override
    public ApiResponse<OrderDto.OrderResponse> createOrder(
            @RequestHeader(value = "X-USER-ID") String xUserId,
            @RequestBody OrderDto.CreateOrderRequest request
    ) {
        OrderInfo orderInfo = orderFacade.createOrder(xUserId, request);
        return ApiResponse.success(OrderDto.OrderResponse.from(orderInfo));
    }

    @GetMapping("/")
    @Override
    public ApiResponse<List<OrderDto.OrderResponse>> getOrders(
            @RequestHeader(value = "X-USER-ID") String xUserId
    ) {
        List<OrderInfo> orderInfos = orderFacade.getOrders(xUserId);
        List<OrderDto.OrderResponse> responses = orderInfos.stream()
                .map(OrderDto.OrderResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{orderId}")
    @Override
    public ApiResponse<OrderDto.OrderResponse> getOrder(
            @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = orderFacade.getOrder(orderId);
        return ApiResponse.success(OrderDto.OrderResponse.from(orderInfo));
    }
}

