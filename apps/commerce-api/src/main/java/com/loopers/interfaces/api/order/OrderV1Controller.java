package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> order(
            @RequestHeader("X-USER-ID") String loginId,
            @Valid @RequestBody OrderV1Dto.OrderRequest orderRequest
    ) {
        OrderCommand.CreateOrderCommand createOrderCommand = orderRequest.toCommand(loginId);
        OrderInfo orderInfo = orderFacade.createOrder(createOrderCommand);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);

        return ApiResponse.success(response);
    }

    @GetMapping
    @Override
    public ApiResponse<List<OrderV1Dto.OrderResponse>> getOrdersInfo(
            @RequestHeader("X-USER-ID") String loginId
    ) {
        List<OrderInfo> orderInfo = orderFacade.getOrdersInfo(loginId);
        List<OrderV1Dto.OrderResponse> response = orderInfo.stream()
                .map(OrderV1Dto.OrderResponse::from)
                .toList();

        return ApiResponse.success(response);
    }

    @GetMapping("/{orderId}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> getOrderInfo(
            @RequestHeader("X-USER-ID") String loginId,
            @PathVariable Long orderId
    ) {
        OrderInfo orderInfo = orderFacade.getOrderInfo(loginId, orderId);
        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);

        return ApiResponse.success(response);
    }

}
