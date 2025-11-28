package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @Override
    @PostMapping("/new")
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(OrderV1Dto.OrderRequest request) {

        List<OrderV1Dto.OrderRequest.OrderItemRequest> items = request.items();

        OrderInfo orderInfo = orderFacade.createOrder(
                request.userId(), items
        );

        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);

        return ApiResponse.success(response);
    }

}
