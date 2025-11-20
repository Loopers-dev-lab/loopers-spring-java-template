package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderResultInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderV1Controller implements OrderV1ApiSpec {
    private final OrderFacade orderFacade;

    @PostMapping
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(@RequestBody OrderV1Dto.OrderRequest request) {
        OrderResultInfo info = orderFacade.createOrder(request);

        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(info);

        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> findOrderById(@PathVariable Long id) {
        return null;
    }
}
