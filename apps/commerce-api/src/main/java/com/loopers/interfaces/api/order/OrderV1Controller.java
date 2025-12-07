package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "주문", description = "주문 관련 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @Override
    @GetMapping
    public ApiResponse<OrderV1Dto.OrderListResponse> getOrders(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        List<Order> orders = orderFacade.getOrders(userId);
        OrderV1Dto.OrderListResponse response = OrderV1Dto.OrderListResponse.from(orders);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{id}")
    public ApiResponse<OrderV1Dto.OrderDetailResponse> getOrder(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable(value = "id") Long id
    ) {
        Order order = orderFacade.getOrder(userId, id);
        OrderV1Dto.OrderDetailResponse response = OrderV1Dto.OrderDetailResponse.from(order);
        return ApiResponse.success(response);
    }

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    @PostMapping
    public ApiResponse<OrderV1Dto.OrderCreateResponse> createOrder(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody @Valid OrderV1Dto.OrderCreateRequest request
    ) {
        Order order = orderFacade.createOrder(userId, request);
        OrderV1Dto.OrderCreateResponse response = OrderV1Dto.OrderCreateResponse.from(order);
        return ApiResponse.success(response);
    }
}
