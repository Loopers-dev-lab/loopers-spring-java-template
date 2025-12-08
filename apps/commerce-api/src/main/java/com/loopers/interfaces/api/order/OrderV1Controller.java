package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @Override
    @PostMapping("/new")
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(OrderV1Dto.OrderRequest request) {

        // Presentation DTO → Application DTO 변환
        OrderCommand command = new OrderCommand(
                request.userId(),
                request.items().stream()
                        .map(item -> new OrderCommand.OrderItemCommand(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList(),
                request.couponId(),
                request.paymentType(),
                request.cardType(),
                request.cardNo()
        );

        OrderInfo orderInfo = orderFacade.createOrder(command);

        OrderV1Dto.OrderResponse response = OrderV1Dto.OrderResponse.from(orderInfo);

        return ApiResponse.success(response);
    }

}
