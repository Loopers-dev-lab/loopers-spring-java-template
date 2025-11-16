package com.loopers.application.api.order;

import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.order.Order;
import com.loopers.core.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderV1Api implements OrderV1ApiSpec {

    private final OrderService orderService;

    @Override
    @PostMapping
    public ApiResponse<OrderV1Dto.OrderResponse> order(
            @RequestHeader(name = "X-USER-ID") String userIdentifier,
            @RequestBody OrderV1Dto.OrderRequest request
    ) {
        Order savedOrder = orderService.order(request.toCommand(userIdentifier));

        return ApiResponse.success(new OrderV1Dto.OrderResponse(savedOrder.getOrderId().value()));
    }
}
