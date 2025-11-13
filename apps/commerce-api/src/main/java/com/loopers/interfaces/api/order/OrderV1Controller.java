package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.user.UserId;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @RequestHeader(value = "X-USER-ID") @NotBlank(message = "X-USER-ID는 필수입니다.") UserId userId,
        @Valid @RequestBody OrderV1Dto.CreateOrderRequest request
    ) {
        List<OrderService.OrderItemRequest> items = request.items().stream()
            .map(item -> new OrderService.OrderItemRequest(item.productId(), item.quantity()))
            .collect(Collectors.toList());
        
        OrderInfo info = orderFacade.createOrder(userId, items);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(info);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> getOrder(
        @PathVariable("id") Long id
    ) {
        OrderInfo info = orderFacade.getOrder(id);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(info);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Override
    public ApiResponse<OrderV1Dto.OrdersResponse> getUserOrders(
        @RequestHeader(value = "X-USER-ID") @NotBlank(message = "X-USER-ID는 필수입니다.") UserId userId
    ) {
        List<OrderInfo> orders = orderFacade.getUserOrders(userId);
        OrderV1Dto.OrdersResponse response = OrderV1Dto.OrdersResponse.from(orders);
        return ApiResponse.success(response);
    }
}

