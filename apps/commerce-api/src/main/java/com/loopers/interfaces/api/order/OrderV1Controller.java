package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderCommand.OrderItemRequest;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping("/users/{userId}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @PathVariable String userId,
        @Valid @RequestBody OrderV1Dto.CreateOrderRequest request
    ) {
        OrderCommand.Create command = new OrderCommand.Create(
            request.orderItems().stream()
                .map(item -> new OrderItemRequest(item.productId(), item.quantity()))
                .toList(),
            request.userCouponId()
        );

        OrderInfo orderInfo = orderFacade.createOrder(userId, command);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);
        return ApiResponse.success(response);
    }

    @GetMapping("/{orderId}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> getOrder(
        @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = orderFacade.getOrder(orderId);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}")
    @Override
    public ApiResponse<Page<OrderV1Dto.OrderResponse>> getOrdersByUser(
        @PathVariable String userId,
        Pageable pageable
    ) {
        Page<OrderInfo> orderInfos = orderFacade.getOrdersByUser(userId, pageable);
        Page<OrderV1Dto.OrderResponse> response = orderInfos.map(
            OrderV1Dto.OrderResponse::from);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{orderId}/users/{userId}")
    @Override
    public ApiResponse<Void> cancelOrder(
        @PathVariable Long orderId,
        @PathVariable String userId
    ) {
        orderFacade.cancelOrder(orderId, userId);
        return ApiResponse.success(null);
    }
}
