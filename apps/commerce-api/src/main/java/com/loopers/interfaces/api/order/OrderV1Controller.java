package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            String userId,
            @Valid OrderV1Dto.PlaceOrderRequest request
    ) {
        OrderPlaceCommand command = request.toCommand(userId);
        OrderInfo orderInfo = orderFacade.placeOrder(command);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }

    @Override
    public ApiResponse<List<OrderV1Dto.OrderResponse>> getOrders(String userId) {
        List<OrderInfo> orderInfos = orderFacade.getMyOrders(userId);
        List<OrderV1Dto.OrderResponse> response = orderInfos.stream()
                .map(OrderV1Dto.OrderResponse::from)
                .toList();

        return ApiResponse.success(response);
    }

    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> getOrderDetail(String userId, Long orderId) {
        OrderInfo orderInfo = orderFacade.getOrderDetail(orderId, userId);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }
}
