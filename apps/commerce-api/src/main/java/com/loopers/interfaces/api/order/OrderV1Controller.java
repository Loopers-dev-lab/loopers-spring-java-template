package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @Override
    @PostMapping
    public ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @RequestHeader("X-USER-ID") String userId,
            @Valid @RequestBody OrderV1Dto.PlaceOrderRequest request
    ) {
        OrderPlaceCommand command = request.toCommand(userId);
        OrderInfo orderInfo = orderFacade.createOrder(command);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }

    @Override
    @GetMapping
    public ApiResponse<List<OrderV1Dto.OrderResponse>> getOrders(
            @RequestHeader("X-USER-ID") String userId
    ) {
        List<OrderInfo> orderInfos = orderFacade.getMyOrders(userId);
        List<OrderV1Dto.OrderResponse> response = orderInfos.stream()
                .map(OrderV1Dto.OrderResponse::from)
                .toList();

        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{orderId}")
    public ApiResponse<OrderV1Dto.OrderResponse> getOrderDetail(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = orderFacade.getOrderDetail(orderId, userId);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }
}
