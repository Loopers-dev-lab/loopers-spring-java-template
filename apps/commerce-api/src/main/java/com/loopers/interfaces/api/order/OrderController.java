package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController implements OrderApiSpec {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderFacade orderFacade;

    @PostMapping("/")
    @Override
    public ApiResponse<Object> createOrder(
            @RequestHeader(value = "X-USER-ID") Long xUserId,
            @RequestBody OrderDto.CreateOrderRequest request
    ) {
        log.info("=== 주문 생성 API 요청 ===");
        log.info("X-USER-ID: {}, request: {}", xUserId, request);
        try {
            orderFacade.createOrder(xUserId, request);
            log.info("주문 생성 API 성공 - userId: {}", xUserId);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("주문 생성 API 실패 - userId: {}, error: {}", xUserId, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/callback")
    @Override
    public ApiResponse<OrderDto.OrderResponse> callbackOrder(
            @RequestBody OrderDto.PgCallbackRequest request
    ) {
        log.info("=== Payment Callback ===");
        log.info("Full callback data: {}", request);
        log.info("========================");
        OrderInfo orderInfo = orderFacade.callbackOrder(request);
        return ApiResponse.success(OrderDto.OrderResponse.from(orderInfo));
    }

    @GetMapping("/")
    @Override
    public ApiResponse<List<OrderDto.OrderResponse>> getOrders(
            @RequestHeader(value = "X-USER-ID") Long xUserId
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

